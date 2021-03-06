package bndtools.launch.ui;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.launch.LaunchConstants;

public class OSGiJUnitLaunchTab extends GenericStackedLaunchTab {

    private Image image = null;

    @Override
    protected ILaunchTabPiece[] createStack() {
        return new ILaunchTabPiece[] {
                new ProjectLaunchTabPiece(),
                new JUnitOutputLaunchTabPiece(),
                new JUnitTestParamsLaunchTabPiece(),
                new LoggingLaunchTabPiece()
        };
    }

    public String getName() {
        return "OSGi Tests";
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        super.performApply(configuration);
        configuration.setAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES, false);
    }

    @Override
    public Image getImage() {
        synchronized (this) {
            if (image == null) {
                image = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
            }
        }
        return image;
    }

    @Override
    public void dispose() {
        super.dispose();
        synchronized (this) {
            if (image != null)
                image.dispose();
        }
    }
}