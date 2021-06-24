package org.wangzw.plugin.cppstyle.ui;

/**
 * Constant definitions for plug-in
 */
public class CppStyleConstants {
	public static final String PerfPageId = "org.wangzw.plugin.cppstyle.ui.CppStylePerfPage";

	public static final String CLANG_FORMAT_PATH = "cppstyle.clangformat.path";
	public static final String CLANG_FORMAT_STYLE_PATH = "cppstyle.clangformat.style.path";
	public static final String CPPLINT_PATH = "cppstyle.cpplint.path";
	public static final String ENABLE_CPPLINT_ON_SAVE = "cppstyle.enable.cpplint.on.save";
	public static final String ENABLE_CLANGFORMAT_ON_SAVE = "cppstyle.enable.clangformat.on.save";

	public static final String LABEL_CLANG_FORMAT_PATH = "Clang-format path:";
	public static final String LABEL_CLANG_FORMAT_STYLE_PATH = "Clang-format style path:";
	public static final String LABEL_CPPLINT_PATH = "Cpplint path:";
	
	public static final String ENABLE_CPPLINT_TEXT = "Enable cpplint";
	public static final String ENABLE_CLANGFORMAT_TEXT = "Run clang-format on file save";
	public static final String PROJECT_ROOT_TEXT = "Select root path for cpplint (optional):";

	public static final String PROJECTS_PECIFIC_PROPERTY = "cppstyle.ENABLE_PROJECTS_PECIFIC";
	public static final String ENABLE_CPPLINT_PROPERTY = "cppstyle.ENABLE_CPPLINT";
	public static final String ENABLE_CLANGFORMAT_PROPERTY = "cppstyle.ENABLE_CLANGFORMAT";
	public static final String CPPLINT_PROJECT_ROOT = "cppstyle.PROJECT_ROOT";

	public static final String CPPLINT_MARKER = "org.wangzw.plugin.cppstyle.CpplintMarker";
	public static final String CONSOLE_NAME = "CppStyle Output";
	public static final String CPPLINT_CONSOLE_PREFIX = "cpplint.py: ";
	public static final String CPPLINT_OUTPUT_PATTERN = "(.+)\\:(\\d+)\\:(.+)\\[(.+)/(.+)\\](.*)\\[(\\d)\\]";
	public static final int CPPLINT_OUTPUT_PATTERN_PATH_GROUP = 1;
	public static final int CPPLINT_OUTPUT_PATTERN_LINE_NO_GROUP = 2;
	public static final int CPPLINT_OUTPUT_PATTERN_MSG_GROUP = 3;
	public static final int CPPLINT_OUTPUT_PATTERN_CATEGORY_GROUP = 4;
	public static final int CPPLINT_OUTPUT_PATTERN_CATEGORY_SUBGROUP = 5;
	public static final int CPPLINT_OUTPUT_PATTERN_SEVERITY_GROUP = 7;

	public static final String CPPLINT_OUTPUT_PARSER_ID = "org.wangzw.plugin.cppstyle.CpplintErrorParser";
	public static final String CPPLINT_PROBLEM_ID_KEY = "id";
	public static final String CPPLINT_ERROR_PROBLEM_ID = "org.wangzw.plugin.cppstyle.cpplint.build.categorized";

}
