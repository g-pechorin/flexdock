/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flexdock.demos.raw.jmf;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.Time;

import org.flexdock.docking.DockingStub;
import org.flexdock.util.ResourceManager;


/**
 * @author Christopher Butler
 */
public class MediaPanel extends Panel implements DockingStub {
    private Player player;
    private Label titlebar;
    private String dockingId;

    public MediaPanel(String id, String title, String mediaFileName) {
        dockingId = id;
        setLayout(new BorderLayout());

        titlebar = new Label(title);
        titlebar.setBackground(new Color(183, 201, 217));
        add(titlebar, BorderLayout.NORTH);

        player = createPlayer(mediaFileName);
        if (player != null) {
            Component viewscreen = player.getVisualComponent();
            Component controls = player.getControlPanelComponent();

            if (viewscreen != null) {
                add(viewscreen, BorderLayout.CENTER);
            } else {
                TextArea ta = new TextArea("No JMF video playback support for '" + mediaFileName + "'");
                ta.setEditable(false);
                add(ta, BorderLayout.CENTER);
            }
            if (controls != null) {
                add(controls, BorderLayout.SOUTH);
            }
        } else {
            TextArea ta = new TextArea("No JMF decoder support for '" + mediaFileName + "'");
            ta.setEditable(false);
            add(ta, BorderLayout.CENTER);
        }
    }

    private static synchronized Player createPlayer(String mediaUri) {
        try {
            URL url = replaceJarUrlWithFileUrl(ResourceManager.getResource(mediaUri), mediaUri);

            MediaLocator locator = new MediaLocator(url);
            final Player mediaPlayer = Manager.createRealizedPlayer(locator);

            // add a listener to put us in an infinite loop
            mediaPlayer.addControllerListener(new ControllerListener() {
                @Override
                public void controllerUpdate(ControllerEvent evt) {
                    if(evt instanceof EndOfMediaEvent) {
                        mediaPlayer.setMediaTime(new Time(0));
                        mediaPlayer.start();
                    }
                }
            });
            return mediaPlayer;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static HashMap jarUrlToTempFileCache = new HashMap();

    /**
     * JMF doesn't realize a player/viewer for URLs pointing to resources inside jars.
     * To workaround this problem, the jar resource will be written to a temp file
     * and the URL replaced with an url pointing to the temp file.
     * I'd promote this into ResourceManager, but it appears to be isolated to JMF, so it'll stay here for now.
     *
     * @param url url with jar in i
     * @param mediaUri name of the original resource in t
     * @return
     * @throws IOException
     */
    private static URL replaceJarUrlWithFileUrl(URL url, String mediaUri) throws IOException {
        if(!url.getProtocol().equals("jar")) {
            return url;
        }

        File f;
        synchronized(jarUrlToTempFileCache) {
            f = (File)jarUrlToTempFileCache.get(url);
            if(f != null && f.exists() && f.isFile() && f.canRead()) {
                return f.toURL();
            }

            f = File.createTempFile("flexdock-", mediaUri);
            f.deleteOnExit();

            FileOutputStream fos = new FileOutputStream(f);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] b = new byte[4096];

            URLConnection conn = url.openConnection();
            InputStream is = conn.getInputStream();
            int read;
            while((read=is.read(b, 0, b.length)) != -1) {
                bos.write(b, 0, read);
            }

            bos.close();

            jarUrlToTempFileCache.put(url, f);
        }
        return f.toURL();
    }

    @Override
    protected void finalize() {
        if(player!=null) {
            player.stop();
            player.close();
            player = null;
        }
    }

    @Override
    public Component getDragSource() {
        return titlebar;
    }

    @Override
    public Component getFrameDragSource() {
        return titlebar;
    }

    @Override
    public String getPersistentId() {
        return dockingId;
    }

    @Override
    public String getTabText() {
        return titlebar.getText();
    }
}
