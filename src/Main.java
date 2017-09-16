import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	static String namePattern = "[a-zA-Z_0-9]*";
	static String funcPattern = "(\\x2A\\x2F\r?\n)?" + namePattern + " (" + namePattern + ")\\(" + "(" + namePattern
			+ " (" + namePattern + ")(,[ \r\n\t]*)?)*\\)";

	static String template = "/*\r\n" + " * Function Name: \r\n" + " *\r\n" + " * Description:  \r\n" + " *\r\n"
			+ " * Parametrs:\r\n" + " *  Input:\r\n" + " *  Output:\r\n" + " *\r\n" + " * Return values:\r\n"
			+ " *  \r\n" + " * Notes:\r\n" + " * \r\n" + " */\n";

	static int nameOffset;
	static int outputOffset;

	static String summary = "Documents Added:\r\n" + "\r\n" + "\r\n" + "No Documents Added:";

	static int addedOffset;
	static int notAddedOffset;
	static int summaryOffsetForAddedCategory = 0;
	static int summaryOffsetForNotAddedCategory = 0;

	static String lastFileInAddedCategory = "";
	static String lastFileInNotAddedCategory = "";

	public static String insertToFinalOutput(String newDocumentation, int index, String finalOutput,
			int finalOutputOffset) {
		String start = finalOutput.substring(0, index + finalOutputOffset);
		String end = finalOutput.substring(index + finalOutputOffset);
		finalOutput = start + newDocumentation + end;
		return finalOutput;
	}

	public static void insertToSummary(String fileName, String funcName, boolean added) {
		int offset = added ? (addedOffset + summaryOffsetForAddedCategory) : (notAddedOffset + summaryOffsetForNotAddedCategory);
		boolean needFileHeadline = added ? (!fileName.equals(lastFileInAddedCategory))
				: (!fileName.equals(lastFileInNotAddedCategory));
		String start = summary.substring(0, offset);
		String end = summary.substring(offset);
		String addedLine = needFileHeadline ? ("\r\n* File: " + fileName + "\r\n       " + funcName)
				: ("\r\n       " + funcName);
		summary = start + addedLine + end;
		if (added) {
			lastFileInAddedCategory = fileName;
			summaryOffsetForAddedCategory += addedLine.length();
		} else {
			lastFileInNotAddedCategory = fileName;
		}
		summaryOffsetForNotAddedCategory += addedLine.length(); //In both cases
	}

	public static String createDocument(String name, String[] params) {
		String ans = template;
		String start = ans.substring(0, nameOffset);
		String end = ans.substring(nameOffset);
		ans = start + name + end;

		int paramsLength = 0;
		for (int i = 0; i < params.length; i++) {
			start = ans.substring(0, outputOffset + paramsLength + name.length());
			end = ans.substring(outputOffset + paramsLength + name.length());
			ans = start + params[i] + end;
			paramsLength += params[i].length();
		}

		return ans;
	}

	private static String[] generateParams(String txt) {
		String[] temp = txt.substring(txt.indexOf('(') + 1, txt.length() - 1).replaceAll("[ \t\n\r,]+", ",")
				.replace(" ", ",").split(",");
		int length = temp.length;
		String[] ans = new String[length / 2 + 1];
		ans[0] = "\r\n";
		int j = 1;
		for (int i = 1; i < length; i = i + 2) {
			ans[j] = " *      " + temp[i] + " -\r\n";
			j++;
		}
		ans[j - 1] = ans[j - 1].replace("\r\n", "");
		return ans;
	}

	public static String attachDocuments(String fileName, String fileContent) {
		String finalOutput = fileContent;
		int finalOutputOffset = 0;
		Pattern pattern = Pattern.compile(funcPattern);
		Matcher matcher = pattern.matcher(fileContent);
		// Check all occurrences
		while (matcher.find()) {
			if (matcher.group(1) != null) {
				insertToSummary(fileName, matcher.group(2), false);
				continue;
			}
			insertToSummary(fileName, matcher.group(2), true);
			String[] params = generateParams(matcher.group());
			String currentDocument = createDocument(matcher.group(2), params);
			finalOutput = insertToFinalOutput(currentDocument, matcher.start(), finalOutput, finalOutputOffset);
			finalOutputOffset += currentDocument.length();
		}
		return finalOutput;
	}

	public static void createAndWrite(String fileName, String data) {
		try {
			FileOutputStream out = new FileOutputStream("DocumentedFiles/" + fileName);
			out.write(data.getBytes());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String loadFile(String filePath) {
		String fileContent = "";
		try {
			fileContent = new String(Files.readAllBytes(Paths.get(filePath)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileContent;
	}

	public static ArrayList<String> getFilesNames() {
		ArrayList<String> ans = new ArrayList<>();
		File curDir = new File(".");
		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isFile() && f.getName().contains(".c")) {
				ans.add(f.getName());
			}
		}
		return ans;
	}

	public static void calculateTemplatesOffsets() {
		String name = "Function Name: ";
		nameOffset = template.indexOf(name) + name.length();

		String output = "Output:";
		outputOffset = template.indexOf(output) + output.length();

		String added = "Documents Added:";
		addedOffset = summary.indexOf(added) + added.length();

		String notAdded = "No Documents Added:";
		notAddedOffset = summary.indexOf(notAdded) + notAdded.length();
	}

	public static void main(String[] args) {

		new File("DocumentedFiles").mkdir();

		calculateTemplatesOffsets();

		ArrayList<String> files = getFilesNames();

		for (String file : files) {
			String fileContent = loadFile(file);
			String finalOutput = attachDocuments(file, fileContent);
			createAndWrite(file, finalOutput);
		}

		createAndWrite("summary.txt", summary);
	}

}
