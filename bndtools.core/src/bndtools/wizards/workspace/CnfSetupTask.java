package bndtools.wizards.workspace;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import bndtools.LocalRepositoryTasks;
import bndtools.Plugin;

/**
 * This subclass of WorkspaceModifyOperation, implements the abstract method
 * execute, which updates the resource hierarchy of the workspace to create the
 * cnf project used by bndtools.
 * 
 * @author bwinspur
 * 
 */
class CnfSetupTask extends WorkspaceModifyOperation {

	private final boolean skipRepoContent;

	/**
	 * Creates a new cnf project and does not skip install of implicit
	 * repository content
	 */
	public CnfSetupTask() {
		this(false); // will not skip install of implicit repository content
	}
	
	/**
	 * Creates a new cnf project and may skip install of implicit
	 * repository content, depending on argument.
	 */
	public CnfSetupTask(boolean skipRepoContent) {
		this.skipRepoContent = skipRepoContent;
	}

	/**
	 * 
	 * org.eclipse.ui.actions.WorkspaceModifyOperation
	 */
	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		final MultiStatus status =
				new MultiStatus(
						Plugin.PLUGIN_ID,
						0,
						"Problems occurred while configuring the Bnd workspace.",
						null); 
		SubMonitor progress =
				SubMonitor
						.convert(monitor, "Copying files to repository...", 4);

		LocalRepositoryTasks.configureBndWorkspace(progress.newChild(1, 0));
		LocalRepositoryTasks.installImplicitRepositoryContents(skipRepoContent,
				status, progress.newChild(2, 0));
		LocalRepositoryTasks.refreshWorkspaceForRepository(progress.newChild(1,
				0));
		if (!status.isOK())
			throw new CoreException(status);
	}

}