package fr.cea.lib;

import java.io.PrintStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.BuildEvent;

public class AntLogger implements BuildLogger, org.apache.tools.ant.SubBuildListener {

	// CLASS DIRECTORY
	private class Dir {

		String  name;
		boolean logged = false;

		java.util.Stack<String>            messages          = new java.util.Stack<String>();

		Dir(String name) {
			this.name = name;
		}
	}


	private PrintStream                        out;
	private PrintStream                        err;
	private boolean                            emacsMode         = true;
	private int                                msg_level         = Project.MSG_INFO;
	private java.util.Stack<Dir>               dirs              = new java.util.Stack<Dir>();
	private boolean                            leaving_directory = false;
	private java.util.Set<java.lang.Throwable> exceptions        = new java.util.HashSet<java.lang.Throwable>();

	// GET DIRECTORY
	private String getDirectory(final BuildEvent event) {
		Project project = event.getProject();
		String dir = project.getBaseDir().getAbsolutePath();
		return dir;
	}

	// ENTER DIRECTORY
	private void enterDirectory(final BuildEvent event) {
		Project project = event.getProject();
		this.dirs.push(new Dir(this.getDirectory(event)));
	}

	// LEAVE DIRECTORY
	private void leaveDirectory(final BuildEvent event) {

		Project project = event.getProject();

		if ( ! this.dirs.empty()) {
			Dir dir = this.dirs.peek();

			if ( ! dir.messages.empty()) {

				// print stack of visited directories
				for (int i = 0 ; i < this.dirs.size() ; ++i) {
					Dir d = this.dirs.elementAt(i);
					if ( ! d.logged) {
						this.out.println("ant[" + (i + 1) + "]: Entering directory " + d.name);
						d.logged = true;
					}
				}

				// print stack of messages for top directory
				java.util.Iterator i = dir.messages.iterator();
				while (i.hasNext()) {
					String msg = (String)i.next();
					this.out.println(msg);
				}
			}

			// print message for leaving top directory
			if (dir.logged)
				this.out.println("ant[" + this.dirs.size() + "]: Leaving directory " + dir.name);

			// pop top directory
			this.dirs.pop();
		}

		this.leaving_directory = false;
	}

	// CHECK EXCEPTION
	private void checkException(final BuildEvent event) {
		java.lang.Throwable ex = event.getException();
		if (ex != null && ! this.exceptions.contains(ex)) {
			Project project = event.getProject();
			project.log(ex.toString(), Project.MSG_ERR);
			this.exceptions.add(ex);
		}
	}

	// START BUILD
	public final void buildStarted(final BuildEvent event) {
		this.enterDirectory(event);
	}

	// FINISH BUILD
	public void buildFinished(final BuildEvent event) {
		// leave all remaining directories
		while (this.dirs.size() > 0)
			this.leaveDirectory(event);
		this.checkException(event);
	}

	// START SUB-BUILD
	public void subBuildStarted(final BuildEvent event) {
	}

	// FINISH SUB-BUILD
	public void subBuildFinished(final BuildEvent event) {
		this.checkException(event);
	}

	// START TARGET
	public void targetStarted(final BuildEvent event) {
	}

	// FINISH TARGET
	public void targetFinished(final BuildEvent event) {
		this.checkException(event);
	}

	// START TASK
	public void taskStarted(final BuildEvent event) {
		String dir = this.getDirectory(event);

		// staying in same directory
		if (this.leaving_directory && dir.equals(this.dirs.peek().name)) {
			this.leaving_directory = false;
			return;
		}

		// leaving previous directory
		if (this.leaving_directory)
			this.leaveDirectory(event);

		// entering new directory
		if (this.dirs.empty() || ! dir.equals(this.dirs.peek().name))
			this.enterDirectory(event);
	}

	// FINISH TASK
	public void taskFinished(final BuildEvent event) {
		// leaving directory
		if ( ! this.dirs.empty())
			this.leaving_directory = true;
		this.checkException(event);
	}

	public final void messageLogged(final BuildEvent event) {
		if (event.getPriority() <= this.msg_level) {
			if (this.dirs.empty())
				this.out.println(event.getMessage());
			else
				this.dirs.peek().messages.push(event.getMessage());
		}
	}

	public final void setOutputPrintStream(final PrintStream output) {
		this.out = new PrintStream(output, true);
	}

	public final void setErrorPrintStream(final PrintStream errorStream) {
		this.err = new PrintStream(errorStream, true);
	}

	public final void setEmacsMode(final boolean mode) {
		this.emacsMode = mode;
	}

	public void setMessageOutputLevel(final int level) {
		this.msg_level = level;
	}

}
