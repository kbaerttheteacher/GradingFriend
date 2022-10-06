package gradingBuddy.gradingPartner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

//Written by K Baert
//V 1.0 2/9/22
//For the purpose of sending the code.org assessment results to students
public class Grader {

	public static void main(String[] args) throws IOException {

		Student[] students = readInStudents();

		ScoreLine[] scoreLines = readInScores(students);

		StudentScore[] studentScores = flattenScores(scoreLines, students);

		createHTML(studentScores);

		sendEmail(studentScores);

	}

	public static Student[] readInStudents() throws IOException {
		Student[] students = new Student[CustomProperties.numOfStudents];

		FileInputStream file = new FileInputStream(new File(CustomProperties.studentEmailLocation));
		Workbook workbook = new XSSFWorkbook(file);
		Sheet sheet = workbook.getSheetAt(CustomProperties.sheetPosition);
		workbook.close();

		int i = 0;

		for (Row row : sheet) {
			Student student = new Student();
			student.setUserName(row.getCell(0).getStringCellValue());
			student.setEmail(row.getCell(1).getStringCellValue());
			students[i] = student;
			i++;
		}

		return students;
	}

	public static ScoreLine[] readInScores(Student[] students) throws IOException {
		ScoreLine[] scores = new ScoreLine[CustomProperties.numOfStudents * CustomProperties.numOfQuestions];

		try (BufferedReader br = new BufferedReader(new FileReader(CustomProperties.assessmentsLocation))) {
			String line;
			int i = 0;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				ScoreLine sl = new ScoreLine();

				sl.setUserName(values[CustomProperties.userNamePosition]);
				sl.setQuestionNum(Integer.valueOf(CustomProperties.questionNumPosition));
				sl.setResponse(values[CustomProperties.responsePosition]);
				sl.setStatus(values[CustomProperties.statusPosition]);
				scores[i] = sl;
				i++;
			}
		}

		return scores;
	}

	public static StudentScore[] flattenScores(ScoreLine[] scoreLines, Student[] students) {

		StudentScore[] studentScores = new StudentScore[CustomProperties.numOfStudents];
		int count = 0;

		for (int i = 0; i < scoreLines.length; i += CustomProperties.numOfQuestions) {
			ScoreLine[] currScoreLines = new ScoreLine[CustomProperties.numOfQuestions];
			for (int j = 0; j < CustomProperties.numOfQuestions; j++) {
				currScoreLines[j] = scoreLines[i + j];
			}

			StudentScore ss = new StudentScore();
			ss.setStudent(lookupStudent(currScoreLines[1].getUserName(), students));
			ss.setScoreLines(currScoreLines);

			studentScores[count] = ss;
			count++;
		}

		return studentScores;

	}

	public static Student lookupStudent(String username, Student[] students) {
		for (int i = 0; i < students.length; i++) {
			if (username.equals(students[i].getUserName())) {
				return students[i];
			}
		}
		return null;
	}

	public static MimeMessage createEmail(String toEmailAddress, String fromEmailAddress, String subject,
			String bodyText) throws MessagingException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		MimeMessage email = new MimeMessage(session);

		email.setFrom(new InternetAddress(fromEmailAddress));
		email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(toEmailAddress));
		email.setSubject(subject);
		email.setText(bodyText);
		return email;
	}

	public static void sendEmail(StudentScore[] studentScores) {

		String host = "smtp.gmail.com";

		Properties properties = System.getProperties();

		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", "465");
		properties.put("mail.smtp.ssl.enable", "true");
		properties.put("mail.smtp.auth", "true");

		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {

			protected PasswordAuthentication getPasswordAuthentication() {

				return new PasswordAuthentication(CustomProperties.fromEmail, CustomProperties.emailPassword);

			}

		});

		try {

			for (int i = 0; i < studentScores.length; i++) {

				MimeMessage message = new MimeMessage(session);

				message.setFrom(new InternetAddress(CustomProperties.fromEmail));

				if (CustomProperties.useDefaultToEmail) {
					message.addRecipient(Message.RecipientType.TO,
							new InternetAddress(CustomProperties.defaultToEmail));
				} else {
					message.addRecipient(Message.RecipientType.TO,
							new InternetAddress(studentScores[i].getStudent().getUserName()));
				}
				message.addRecipient(Message.RecipientType.CC, new InternetAddress(CustomProperties.defaultToEmail));

				message.setSubject(CustomProperties.subject);

				System.out.println("trying to send message " + studentScores[i].getStudent().getUserName());

				message.setContent("Hello " + studentScores[i].getStudent().getUserName() + studentScores[i].getHtml(),
						"text/html");
				Transport.send(message);

				System.out.println("sent message " + studentScores[i].getStudent().getUserName());
			}
		} catch (MessagingException mex) {
			mex.printStackTrace();
		}

	}

	public static void createHTML(StudentScore[] studentScores) {
		for (int i = 0; i < studentScores.length; i++) {
			studentScores[i].setHtml(GenerateHTML.generateHTML(studentScores[i]));
		}
	}

}
