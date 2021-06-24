package org.wangzw.plugin.cppstyle.ui;

import static org.wangzw.plugin.cppstyle.ui.CppStyleConstants.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wangzw.plugin.cppstyle.ClangPathHelper;
import org.wangzw.plugin.cppstyle.CppStyle;
import org.wangzw.plugin.cppstyle.FilePathUtil;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class CppStylePerfPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private FileFieldEditor clangFormatPath = null;
	private FileFieldEditor clangFormatStylePath = null;
	private FileFieldEditor cpplintPath = null;
	private BooleanFieldEditor enableCpplintOnSave = null;
	private BooleanFieldEditor enableClangFormatOnSave = null;
	private ClangPathHelper clangPathHelper = null;

	public CppStylePerfPage() {
		super(GRID);
		setPreferenceStore(CppStyle.getDefault().getPreferenceStore());
		clangPathHelper = new ClangPathHelper();
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	public void createFieldEditors() {
		clangFormatPath = createClangPathEditorField();
		addField(clangFormatPath);

		clangFormatStylePath = createClangFormatStylePathEditorField();
		addField(clangFormatStylePath);
		
		cpplintPath =  createCpplintPathEditorField();
		addField(cpplintPath);
		
		enableCpplintOnSave = createEnableCpplintOnSaveButton();
		addField(enableCpplintOnSave);

		enableClangFormatOnSave = createEnableClangFormatOnSaveButton();
		addField(enableClangFormatOnSave);
	}

	public void init(IWorkbench workbench) {
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);

		if (event.getProperty().equals(FieldEditor.VALUE)) {
			String newValue = event.getNewValue().toString();
			if (event.getSource() == clangFormatPath) {
			    pathChange(LABEL_CLANG_FORMAT_PATH, newValue);
			}
			else if (event.getSource() == clangFormatStylePath) {
			    pathChange(LABEL_CLANG_FORMAT_STYLE_PATH, newValue);
			} else if (event.getSource() == cpplintPath) {
				pathChange(LABEL_CPPLINT_PATH, newValue);
			}

			checkState();
		}
	}
	
    private void pathChange(String propertyLable, String newPath) {
        List<String> newPathCandidates = FilePathUtil.resolvePaths(newPath);
        Optional<String> validPath = Optional.empty();
        FieldEditor fieldEditor = null;
        if (LABEL_CLANG_FORMAT_PATH.equals(propertyLable)) {
            validPath = clangPathHelper.getFirstValidClangFormatPath(newPathCandidates);
            fieldEditor = enableClangFormatOnSave;
        }
        else if (LABEL_CLANG_FORMAT_STYLE_PATH.equals(propertyLable)) {
            validPath = clangPathHelper.getFirstValidClangFormatStylePath(newPathCandidates);
        }
        else if (LABEL_CPPLINT_PATH.equals(propertyLable)) {
            validPath = clangPathHelper.getFirstValidCpplintPath(newPathCandidates);
            fieldEditor = enableCpplintOnSave;
        }
        
        boolean isValid = validPath.isPresent();        
        String msg = isValid ? null : propertyLable + " None of the candidates exist \"" + newPath + "\"";

        this.setValid(isValid);
		this.setErrorMessage(msg);
		
		if(fieldEditor != null) {
			fieldEditor.setEnabled(isValid, getFieldEditorParent());
		}
    }

    private FileFieldEditor createClangPathEditorField() {
        return createFileFieldEditorWithEnvironmentVariableSupport(
                CLANG_FORMAT_PATH, LABEL_CLANG_FORMAT_PATH, getFieldEditorParent());
    }

    private FileFieldEditor createClangFormatStylePathEditorField() {
        return createFileFieldEditorWithEnvironmentVariableSupport(
                CLANG_FORMAT_STYLE_PATH, LABEL_CLANG_FORMAT_STYLE_PATH, getFieldEditorParent());
    }

    private FileFieldEditor createCpplintPathEditorField() {
		return createFileFieldEditorWithEnvironmentVariableSupport(
				CPPLINT_PATH, LABEL_CPPLINT_PATH, getFieldEditorParent());
	}

	private BooleanFieldEditor createEnableClangFormatOnSaveButton() {
		BooleanFieldEditor enableClangFormatOnSave = new BooleanFieldEditor(ENABLE_CLANGFORMAT_ON_SAVE,
				ENABLE_CLANGFORMAT_TEXT, getFieldEditorParent());
	
		if (clangPathHelper.getCachedClangFormatPath() == null) {
			enableClangFormatOnSave.setEnabled(false, getFieldEditorParent());
		}
		return enableClangFormatOnSave;
	}

	private BooleanFieldEditor createEnableCpplintOnSaveButton() {
		BooleanFieldEditor enableCpplintOnSave = new BooleanFieldEditor(ENABLE_CPPLINT_ON_SAVE,
				ENABLE_CPPLINT_TEXT, getFieldEditorParent());
	
		if (clangPathHelper.getCachedCpplintPath() == null) {
			enableCpplintOnSave.setEnabled(false, getFieldEditorParent());
		}
		return enableCpplintOnSave;
	}

	private FileFieldEditor createFileFieldEditorWithEnvironmentVariableSupport(
            String preferenceName, String label, Composite parentComposite) {
        return new FileFieldEditor(preferenceName, label, parentComposite) {
            @Override
            protected boolean checkState() {
                String pathCandidates = getTextControl().getText();
                String msg = checkPathCandidates(pathCandidates);

                if (msg != null) { // error
                    showErrorMessage(msg);
                    return false;
                }

                if (doCheckState()) { // OK!
                    clearErrorMessage();
                    return true;
                }
                msg = getErrorMessage(); // subclass might have changed it in the #doCheckState()
                if (msg != null) {
                    showErrorMessage(msg);
                }
                return false;
            }

            private String checkPathCandidates(String pathCandidates) {
                String msg = null;
                List<String> resolvedPathCandidates = FilePathUtil.resolvePaths(pathCandidates);
                for (String candidate : resolvedPathCandidates) {
                    if (candidate.isEmpty()) {
                        if (!isEmptyStringAllowed()) {
                            msg = getErrorMessage();
                        }
                    }
                    else {
                        File file = new File(candidate);
                        if (file.exists()) {
                            // is valid
                            msg = null;
                            break;
                        }
                        else {
                            msg = getErrorMessage();
                        }
                    }
                }
                return msg;
            }
        };
    }
}