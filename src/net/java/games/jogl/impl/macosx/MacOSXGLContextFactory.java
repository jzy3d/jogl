/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package net.java.games.jogl.impl.macosx;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class MacOSXGLContextFactory extends GLContextFactory {
  static {
    NativeLibLoader.load();
  }

  public GraphicsConfiguration chooseGraphicsConfiguration(GLCapabilities capabilities,
                                                           GLCapabilitiesChooser chooser,
                                                           GraphicsDevice device) {
    return null;
  }

  public GLDrawable getGLDrawable(Object target,
                                  GLCapabilities capabilities,
                                  GLCapabilitiesChooser chooser) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    if (!(target instanceof Component)) {
      throw new IllegalArgumentException("GLDrawables not supported for objects of type " +
                                         target.getClass().getName() + " (only Components are supported in this implementation)");
    }
    return new MacOSXOnscreenGLDrawable((Component) target, capabilities, chooser);
  }

  public GLDrawableImpl createOffscreenDrawable(GLCapabilities capabilities,
                                                GLCapabilitiesChooser chooser) {
    return new MacOSXOffscreenGLDrawable(capabilities);
  }

  public boolean canCreateGLPbuffer(GLCapabilities capabilities,
                                    int initialWidth,
                                    int initialHeight) {
    return true;
  }

  public GLPbuffer createGLPbuffer(final GLCapabilities capabilities,
                                   final int initialWidth,
                                   final int initialHeight,
                                   final GLContext shareWith) {
    final List returnList = new ArrayList();
    Runnable r = new Runnable() {
        public void run() {
          MacOSXPbufferGLDrawable pbufferDrawable = new MacOSXPbufferGLDrawable(capabilities,
										initialWidth,
										initialHeight);
          GLPbufferImpl pbuffer = new GLPbufferImpl(pbufferDrawable, shareWith);
          returnList.add(pbuffer);
        }
      };
    maybeDoSingleThreadedWorkaround(r);
    return (GLPbuffer) returnList.get(0);
  }

  private void maybeDoSingleThreadedWorkaround(Runnable action) {
    if (SingleThreadedWorkaround.doWorkaround() && !EventQueue.isDispatchThread()) {
      try {
        EventQueue.invokeAndWait(action);
      } catch (InvocationTargetException e) {
        throw new GLException(e.getTargetException());
      } catch (InterruptedException e) {
        throw new GLException(e);
      }
    } else {
      action.run();
    }
  }
}
