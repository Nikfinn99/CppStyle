package org.wangzw.plugin.cppstyle;

import static org.wangzw.plugin.cppstyle.ui.CppStyleConstants.CLANG_FORMAT_PATH;
import static org.wangzw.plugin.cppstyle.ui.CppStyleConstants.CLANG_FORMAT_STYLE_PATH;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.formatter.CodeFormatter;
import org.eclipse.cdt.ui.ICEditor;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.wangzw.plugin.cppstyle.diff_match_patch.Diff;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;
import org.wangzw.plugin.cppstyle.ui.CppStyleMessageConsole;


public class ClangFormatFormatter extends CodeFormatter {
	
	private static final String ASSUME_FILENAME_CPP = "A.cpp";
	
	private MessageConsoleStream err = null;
	
	private Map<String, ?> options;

    private String clangFormatPath;

    private String clangFormatStylePath;

    private String assumeFilenamePath;

    
    private boolean isClangFormatStylePathValid;

    private boolean isAssumeFilenamePathValid;

    private ClangPathHelper clangPathHelper;
    
	public ClangFormatFormatter() {
		super();
		CppStyleMessageConsole console = CppStyle.buildConsole();
		err = console.getErrorStream();
		
		clangPathHelper = new ClangPathHelper();
        initClangFormatPath();
        initClangFormatStylePath();
        initAssumeFilenamePath();
	    }

	    private void initClangFormatPath() {
	        if (clangPathHelper.getCachedClangFormatPath() == null) {
	            List<String> candidates = getClangFormatPathsFromPreferences();
	            boolean validPathPresent = clangPathHelper.getFirstValidClangFormatPath(candidates).isPresent();
	            if (!validPathPresent) {
	            	Logger.logError("No valid clang-format executable path found");
	            }
	        }
	        clangFormatPath = clangPathHelper.getCachedClangFormatPath();
	    }

	    private void initClangFormatStylePath() {
	        if (clangPathHelper.getCachedClangFormatStylePath() == null) {
	            List<String> candidates = getClangFormatStylePathsFromPreferences();
	            boolean validPathPresent = clangPathHelper.getFirstValidClangFormatStylePath(candidates).isPresent();
	            if (!validPathPresent) {
	            	Logger.logInfo("No valid .clang-format style path found");
	            }
	        }
	        clangFormatStylePath = clangPathHelper.getCachedClangFormatStylePath();
	        isClangFormatStylePathValid = clangFormatStylePath != null;
	    }

	    private void initAssumeFilenamePath() {
	        if (isClangFormatStylePathValid) {
	            assumeFilenamePath = stylePathToAssumeFilenamePath(clangFormatStylePath);
	            isAssumeFilenamePathValid = true;
	        }
	    }

	@Override
	public String createIndentationString(int indentationLevel) {
		return super.createIndentationString(indentationLevel);
	}

	@Override
	public void setOptions(Map<String, ?> options) {
		if (options != null) {
			this.options = options;
		} else {
			this.options = CCorePlugin.getOptions();
		}
	}

	private static IPath getSourceFilePathFromEditorInput(IEditorInput editorInput) {
		if (editorInput instanceof IURIEditorInput) {
			URI uri = ((IURIEditorInput) editorInput).getURI();
			if (uri != null) {
				IPath path = URIUtil.toPath(uri);
				if (path != null) {
					  return path;
				}
			}
		}

		if (editorInput instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) editorInput).getFile();
			if (file != null) {
				return file.getLocation();
			}
		}

		if (editorInput instanceof ILocationProvider) {
			return ((ILocationProvider) editorInput).getPath(editorInput);
		}

		return null;
	}

	@Override
	public TextEdit format(int kind, String source, int offset, int length, int arg4, String lineSeparator) {
		TextEdit retval = format(source, /*getSourceFilePath()*/getAssumeFilenamePath(), new Region(offset, length));
		return retval != null ? retval : new MultiTextEdit();
	}

	public void formatAndApply(ICEditor editor) {
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());

		String path = ((IFileEditorInput) editor.getEditorInput()).getFile().getLocation().toOSString();
		TextEdit res = format(doc.get(), path, null);

		if (res == null) {
			return;
		}

		IDocumentUndoManager manager = DocumentUndoManagerRegistry.getDocumentUndoManager(doc);
		manager.beginCompoundChange();

		try {
			res.apply(doc);
		} catch (MalformedTreeException e) {
			Logger.logError("Failed to apply change", e);
		} catch (BadLocationException e) {
			Logger.logError("Failed to apply change", e);
		}

		manager.endCompoundChange();

	}

	private TextEdit format(String source, String path, IRegion region) {
//		String confPath = getClangFormatConfigureFile(path);
//		if (confPath == null) {
		if (path == null) {
			err.println(
					"Cannot find .clang-format or _clang-format configuration file under any level "
							+ "parent directories of path (" + path + ").");
			err.println("Clang-format will default to Google style.");
		}

		// make clang-format do its own search for the configuration, but fall back to Google.
		String stdArg = "-style=file";
		String fallbackArg = "-fallback-style=Google";

		ArrayList<String> commands = new ArrayList<String>(
				Arrays.asList(clangFormatPath, "-assume-filename=" + path, stdArg, fallbackArg));

		StringBuffer sb = new StringBuffer();
		sb.append(stdArg + " " + fallbackArg + " ");

		if (region != null) {
			commands.add("-offset=" + region.getOffset());
			commands.add("-length=" + region.getLength());

			sb.append("-offset=");
			sb.append(region.getOffset());
			sb.append(" -length=");
			sb.append(region.getLength());
			sb.append(' ');
		}

		ProcessBuilder builder = new ProcessBuilder(commands);

		String root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		builder.directory(new File(root));

		try {
			Logger.logInfo(String.format("Using clang-format: %s with style-file: %s", clangFormatPath, clangFormatStylePath));
			Process process = builder.start();
			OutputStreamWriter output = new OutputStreamWriter(process.getOutputStream());

			output.write(source);
			output.flush();
			output.close();

			InputStreamReader reader = new InputStreamReader(process.getInputStream());
			InputStreamReader error = new InputStreamReader(process.getErrorStream());

			final char[] buffer = new char[1024];
			final StringBuilder stdout = new StringBuilder();
			final StringBuilder errout = new StringBuilder();

			for (;;) {
				int rsz = reader.read(buffer, 0, buffer.length);

				if (rsz < 0) {
					break;
				}

				stdout.append(buffer, 0, rsz);
			}

			for (;;) {
				int rsz = error.read(buffer, 0, buffer.length);

				if (rsz < 0) {
					break;
				}

				errout.append(buffer, 0, rsz);
			}

			String newSource = stdout.toString();

			int code = process.waitFor();
			if (code != 0) {
				err.println("clang-format return error (" + code + ").");
				err.println(errout.toString());
				return null;
			}

			if (errout.length() > 0) {
				err.println(errout.toString());
				return null;
			}

			if (0 == source.compareTo(newSource)) {
				return null;
			}

			diff_match_patch diff = new diff_match_patch();

			LinkedList<Diff> diffs = diff.diff_main(source, newSource);
			diff.diff_cleanupEfficiency(diffs);

			int offset = 0;
			MultiTextEdit edit = new MultiTextEdit();

			for (Diff d : diffs) {
				switch (d.operation) {
				case INSERT:
					InsertEdit e = new InsertEdit(offset, d.text);
					edit.addChild(e);
					break;
				case DELETE:
					DeleteEdit e1 = new DeleteEdit(offset, d.text.length());
					offset += d.text.length();
					edit.addChild(e1);
					break;
				case EQUAL:
					offset += d.text.length();
					break;
				}
			}

			return edit;

		} catch (IOException e) {
			Logger.logError("Failed to format code", e);
		} catch (InterruptedException e) {
			Logger.logError("Failed to format code", e);
		}

		return null;
	}

	 protected List<String> getClangFormatPathsFromPreferences() {
	        return getResolvedPreferenceValues(CLANG_FORMAT_PATH);
	    }

    protected List<String> getClangFormatStylePathsFromPreferences() {
        return getResolvedPreferenceValues(CLANG_FORMAT_STYLE_PATH);
    }

    private List<String> getResolvedPreferenceValues(String preferenceName) {
        String semicolonSeperatedPaths = CppStyle.getDefault().getPreferenceStore().getString(preferenceName);
        return FilePathUtil.resolvePaths(semicolonSeperatedPaths);
    }
    
    protected String getAssumeFilenamePath() {
        if (isAssumeFilenamePathValid) {
            return assumeFilenamePath;
        }
        Logger.logInfo("Trying to find .clang-format style");

        assumeFilenamePath = getSourceFilePathFromActiveEditor();
        if (FilePathUtil.fileExists(assumeFilenamePath)) {
            return assumeFilenamePath;
        }

        assumeFilenamePath = useWorkspaceFallback();
        return assumeFilenamePath;
    }

    private String stylePathToAssumeFilenamePath(String clangFormatStylePath) {
        File assumeFile = new File(new File(clangFormatStylePath).getParentFile(), ASSUME_FILENAME_CPP);
        return assumeFile.getAbsolutePath();
    }

    private String getSourceFilePathFromActiveEditor() {
        String javaFilePath = null;
        IWorkbench wb = PlatformUI.getWorkbench();
        if (wb != null) {
            IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    IEditorPart activeEditor = page.getActiveEditor();
                    if (activeEditor != null) {
                        IEditorInput editorInput = activeEditor.getEditorInput();
                        if (editorInput != null) {
                            IPath filePath = getSourceFilePathFromEditorInput(editorInput);
                            if (filePath != null) {
                                javaFilePath = filePath.toOSString();
                            }
                        }
                    }
                }
            }
        }
        return javaFilePath;
    }

    private String useWorkspaceFallback() {
        String workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
        String assumeFilePath = new File(workspaceRoot, ASSUME_FILENAME_CPP).getAbsolutePath();
        return assumeFilePath;
    }

	public boolean checkClangFormat(String clangformat) {
		if (clangformat == null) {
			err.println("clang-format is not specified.");
			return false;
		}

		File file = new File(clangformat);

		if (!file.exists()) {
			err.println("clang-format (" + clangformat + ") does not exist.");
			return false;
		}

		if (!file.canExecute()) {
			err.println("clang-format (" + clangformat + ") is not executable.");
			return false;
		}

		return true;
	}

	private boolean enableClangFormatOnSave(IResource resource) {
		boolean enable = CppStyle.getDefault().getPreferenceStore()
				.getBoolean(CppStyleConstants.ENABLE_CLANGFORMAT_ON_SAVE);

		try {
			IProject project = resource.getProject();
			String enableProjectSpecific = project
					.getPersistentProperty(new QualifiedName("", CppStyleConstants.PROJECTS_PECIFIC_PROPERTY));

			if (enableProjectSpecific != null && Boolean.parseBoolean(enableProjectSpecific)) {
				String value = project
						.getPersistentProperty(new QualifiedName("", CppStyleConstants.ENABLE_CLANGFORMAT_PROPERTY));
				if (value != null) {
					return Boolean.parseBoolean(value);
				}

				return false;
			}
		} catch (CoreException e) {
			Logger.logError(e);
		}

		return enable;
	}

	public boolean runClangFormatOnSave(IResource resource) {
		boolean canRun = false;
		if (enableClangFormatOnSave(resource)) {
			if (clangFormatPath == null) {
				err.println("clang-format command must be specified in preferences.");
				canRun = false;
			}
			else {
				canRun = true;
			}
		}
		return canRun;
	}

	public static String getClangFormatPath() {
		return CppStyle.getDefault().getPreferenceStore().getString(CLANG_FORMAT_PATH);
	}

}
