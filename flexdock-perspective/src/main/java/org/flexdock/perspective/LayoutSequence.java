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
package org.flexdock.perspective;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.flexdock.docking.Dockable;
import org.flexdock.docking.DockingConstants;
import org.flexdock.docking.DockingManager;
import org.flexdock.docking.DockingPort;
import org.flexdock.docking.state.DockingState;
import org.flexdock.util.DockingUtility;

/**
 * @author Christopher Butler
 */
public class LayoutSequence implements Cloneable, Serializable, DockingConstants {

    private List sequence;  // contains DockingState objects

    public LayoutSequence() {
        this(new ArrayList());
    }

    public LayoutSequence(DockingState[] dockingStates) {
        this(dockingStates == null
             ? new ArrayList()
             : Arrays.asList(dockingStates));
    }

    private LayoutSequence(List list) {
        sequence = list;
    }

    public void add(Dockable dockable) {
        add(dockable, null);
    }

    public void add(String dockable) {
        add(dockable, null);
    }

    public void add(Dockable dockable, Dockable relativeParent) {
        add(dockable, relativeParent, CENTER_REGION, -1.0f);
    }

    public void add(String dockable, String relativeParent) {
        add(dockable, relativeParent, CENTER_REGION, -1.0f);
    }

    public void add(Dockable dockable, Dockable relativeParent, String region, float ratio) {
        String dockableId = dockable==null? null: dockable.getPersistentId();
        String parentId = relativeParent==null? null: relativeParent.getPersistentId();
        add(dockableId, parentId, region, ratio);
    }

    public void add(String dockableId, String relativeParentId, String region, float ratio) {
        if(dockableId==null) {
            return;
        }

        if(relativeParentId==null && sequence.size() > 0) {
            throw new IllegalStateException("All calls to add() after the first dockable has been added MUST specify a relative dockable parent.");
        }

        DockingState info = new DockingState(dockableId);
        info.setRelativeParentId(relativeParentId);
        info.setRegion(region);
        info.setSplitRatio(ratio);
        sequence.add(info);
    }

    //Claudio Romano request
    public void add(DockingState dockingState) {
        if (dockingState == null) {
            return;
        }
        sequence.add(dockingState);
    }

    public void apply(DockingPort port) {
        if(port==null) {
            return;
        }

        boolean listen = PerspectiveManager.isDockingStateListening();
        PerspectiveManager.setDockingStateListening(false);

        PerspectiveManager.clear(port);
        int len = sequence.size();
        Dockable[] dockables = new Dockable[len];
        for(int i=0; i<len; i++) {
            DockingState info = (DockingState)sequence.get(i);
            Dockable dockable = info.getDockable();
            dockables[i] = dockable;
            String region = info.getRegion();
            if(i==0) {
                DockingManager.dock(info.getDockable(), port, info.getRegion());
                continue;
            }

            Dockable parent = info.getRelativeParent();
            float ratio = info.getSplitRatio();
            DockingUtility.dockRelative(dockable, parent, region, ratio);
        }

        PerspectiveManager.setDockingStateListening(listen);
        PerspectiveManager.updateDockingStates(dockables);
    }

    public List getDockingStates() {
        return getSequenceClone();
    }

    private ArrayList getSequenceClone() {
        ArrayList list = new ArrayList(sequence.size());
        for(Iterator it=sequence.iterator(); it.hasNext();) {
            DockingState info = (DockingState)it.next();
            list.add(info.clone());
        }
        return list;
    }

    @Override
    public Object clone() {
        return new LayoutSequence(getSequenceClone());
    }

}
