package com.astraia.jaxb.process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.astraia.jaxb.generated.Bold;
import com.astraia.jaxb.generated.Italic;
import com.astraia.jaxb.generated.Report;
import com.astraia.jaxb.generated.Section;

public class Execute {

	private StringBuilder output = new StringBuilder();
	private boolean isboldOrItalic;
	private boolean isSection;
	private static final String BOLD_CHAR = "'''";
	private static final String ITALIC_CHAR = "''";
	private static final String BOLD_ITALIC_CHAR = "'''''";
	private static final String HEADING_SUPERATOR = "=";
	private static final String NEW_LINE = "\n";
	private static final String URL_SUPERATOR = "/";
	private static final String WIKI = ".wiki";

	public static void main(String[] args) {

		Scanner scanner = null;
		try {
			scanner = new Scanner(System.in);
			System.out.print("please Enter input file location: ");
			String inputurl = scanner.next();

			System.out.print("please Enter output file location: ");
			String outputurl = scanner.next();
			Execute execute = new Execute();
			execute.run(inputurl, outputurl);
		} finally {
			if (scanner != null)
				scanner.close();
		}
	}

	public void run(final String inputFolderName, final String outputFolderName) {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		Runnable periodicTask = new Runnable() {
			public void run() {
				try {
					processFolder(inputFolderName, outputFolderName);
				} catch (JAXBException | IOException e) {
					System.out.println(e.getMessage());
				}
			}
		};
		executor.scheduleAtFixedRate(periodicTask, 0, 5, TimeUnit.SECONDS);
	}

	/**
	 * process all the XML in the folder and prepare it as .wiki
	 * 
	 * @param inputFolderName
	 * @param outputFolderName
	 * @throws JAXBException
	 * @throws IOException
	 */
	public void processFolder(final String inputFolderName, final String outputFolderName)
			throws JAXBException, IOException {
		File inputFolder = new File(inputFolderName);
		File outfolder = new File(outputFolderName);
		if (!outfolder.exists()) {
			outfolder.mkdirs();
		}
		for (final File fileEntry : inputFolder.listFiles()) {
			if (fileEntry.getName().length() > 5
					&& fileEntry.getName().substring(fileEntry.getName().length() - 4).equals(".xml")) {
				JAXBContext jaxbContext = JAXBContext.newInstance(Report.class);
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				Report report = (Report) unmarshaller.unmarshal(fileEntry.getCanonicalFile());
				int count = 1;
				processNode(report.getContent(), count);
				String filename = fileEntry.getName().substring(0, fileEntry.getName().length() - 4) + WIKI;
				File out = new File(outputFolderName + URL_SUPERATOR + filename);
				out.createNewFile();
				FileWriter writer = new FileWriter(out);
				writer.append(output);
				writer.flush();
				writer.close();
				output = new StringBuilder();
				System.out.println("please check your file at " + out.getCanonicalFile());
				fileEntry.delete();
			}

		}

	}

	/**
	 * 
	 * Process each Node and prepare the .wiki file
	 * 
	 * @param object
	 *            current Node(JAXB obj)
	 * @param count
	 *            this is used for to know the current depth to print header Ex-
	 *            ==XXXX==
	 */
	public void processNode(Object object, int count) {
		if (object instanceof List) {
			List<Object> objList = (List<Object>) object;
			if (objList != null && !objList.isEmpty()) {

				objList.forEach(section -> {

					if (section instanceof Section) {
						isSection = true;
						isboldOrItalic = false;
						output.append(NEW_LINE);
						Section sec = (Section) section;
						output.append(prepareHeader(sec.getHeading(), count));
						processNode(sec.getContent(), count + 1);
					} else if (section instanceof Bold) {
						isboldOrItalic = true;
						if (isSection) {
							output.append(NEW_LINE);
						}
						Bold sec = (Bold) section;
						sec.getContent().forEach(op -> {

							if (op instanceof Italic) {
								Italic italic = (Italic) op;
								output.append(ITALIC_CHAR + italic.getContent().get(0));
							} else {
								output.append(BOLD_CHAR + op);
							}

						});
						if (sec.getContent().size() > 1) {
							output.append(BOLD_ITALIC_CHAR);
						} else {
							output.append(BOLD_CHAR);
						}

					} else if (section instanceof Italic) {
						isboldOrItalic = true;
						if (isSection) {
							output.append(NEW_LINE);
						}
						Italic sec = (Italic) section;
						sec.getContent().forEach(op -> {

							if (op instanceof Bold) {
								Bold bold = (Bold) op;
								output.append(BOLD_CHAR + bold.getContent().get(0));
							} else {
								output.append(ITALIC_CHAR + op);
							}

						});
						if (sec.getContent().size() > 1) {
							output.append(BOLD_ITALIC_CHAR);
						} else {
							output.append(ITALIC_CHAR);
						}
					} else {
						if ((count < 2 || isboldOrItalic) && section.toString().trim().length() != 0) {
							output.append(section.toString().trim());
						} else if (!isboldOrItalic && section.toString().trim().length() != 0) {
							isSection = false;
							output.append(NEW_LINE + section.toString().trim());
						}
					}
				});

			}
		}
	}

	private String prepareHeader(String input, int count) {
		String preSuff = IntStream.range(0, count).mapToObj(i -> HEADING_SUPERATOR).collect(Collectors.joining(""));
		return preSuff + input + preSuff;
	}
}
