package bndtools.launch;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.SocketUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.utils.BundleUtils;

public class OSGiJUnitLaunchDelegate extends OSGiLaunchDelegate implements ILaunchConfigurationDelegate2 {

    private static final String JDT_JUNIT_BSN = "org.eclipse.jdt.junit";

    static final String ATTR_JUNIT_PORT = "org.eclipse.jdt.junit.PORT";

    private static final String BNDTOOLS_RUNTIME_JUNIT_BSN = LaunchConstants.JUNIT_PREFIX;

    int port = -1;

    @Override
    public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
        String reporter = configuration.getAttribute(LaunchConstants.ATTR_JUNIT_REPORTER, LaunchConstants.DEFAULT_JUNIT_REPORTER);
        if("port".equals(reporter)) {
            try {
                Bundle jdtJUnitBundle = BundleUtils.findBundle(Plugin.getDefault().getBundleContext(), JDT_JUNIT_BSN, null);
                if(jdtJUnitBundle == null)
                    throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Bundle \"{0}\" was not found. Cannot report JUnit results via the Workbench.", JDT_JUNIT_BSN), null));
                jdtJUnitBundle.start();
            } catch (BundleException e) {
                throw new  CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error starting bundle \"{0}\". Cannot report JUnit results via the Workbench.", JDT_JUNIT_BSN), null));
            }
        }

        ILaunchConfigurationWorkingCopy modifiedConfig = configuration.getWorkingCopy();
        String launchTarget = configuration.getAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, (String) null);
        if(launchTarget != null) {
            IResource launchResource = ResourcesPlugin.getWorkspace().getRoot().findMember(launchTarget);
            IProject launchProject = launchResource.getProject();
            modifiedConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, launchProject.getName());
        }

        return super.getLaunch(modifiedConfig.doSave(), mode);
    }

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        String reporter = configuration.getAttribute(LaunchConstants.ATTR_JUNIT_REPORTER, LaunchConstants.DEFAULT_JUNIT_REPORTER);
        if("port".equals(reporter)) {
            // Find the JUnit port
            port = SocketUtil.findFreePort();
            launch.setAttribute(ATTR_JUNIT_PORT, Integer.toString(port));
        }
        super.launch(configuration, mode, launch, monitor);
    }



    @Override
    protected Properties generateLaunchProperties(ILaunchConfiguration configuration) throws CoreException {
        Properties props = super.generateLaunchProperties(configuration);

        String reporter = configuration.getAttribute(LaunchConstants.ATTR_JUNIT_REPORTER, LaunchConstants.DEFAULT_JUNIT_REPORTER);
        if("port".equals(reporter)) {
            props.setProperty(LaunchConstants.PROP_LAUNCH_JUNIT_REPORTER, "port:" + Integer.toString(port));
        } else {
            props.setProperty(LaunchConstants.PROP_LAUNCH_JUNIT_REPORTER, reporter);
        }

        boolean keepAlive = configuration.getAttribute(LaunchConstants.PROP_LAUNCH_JUNIT_KEEP_ALIVE, true);
        props.setProperty(LaunchConstants.PROP_LAUNCH_JUNIT_KEEP_ALIVE, Boolean.toString(keepAlive));

        // For testing, always clean the framework & use a different storage dir than for runtimes
        props.setProperty(LaunchConstants.PROP_LAUNCH_CLEAN, TRUE.toString());
        props.setProperty(LaunchConstants.PROP_LAUNCH_STORAGE_DIR, LaunchConstants.DEFAULT_LAUNCH_STORAGE_DIR_TEST);

        // Turn off dynamic updates
        props.setProperty(LaunchConstants.PROP_LAUNCH_DYNAMIC_BUNDLES, FALSE.toString());

        // Request shutdown on failure to install/start bundles
        props.setProperty(LaunchConstants.PROP_LAUNCH_SHUTDOWN_ON_ERROR, TRUE.toString());

        return props;
    }

    @Override
    protected Collection<String> calculateRunBundlePaths(Project model) throws CoreException {
        Collection<String> runBundles = super.calculateRunBundlePaths(model);

        File junitBundle = findBundle(model, BNDTOOLS_RUNTIME_JUNIT_BSN, "0");
        if(junitBundle == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Could not find JUnit bundle {0}.", BNDTOOLS_RUNTIME_JUNIT_BSN), null));
        runBundles.add(junitBundle.getAbsolutePath());

        return runBundles;
    }
}