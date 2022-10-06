package gradingBuddy.gradingPartner;

public class StudentScore {

	private Student student;

	private ScoreLine[] scoreLines;

	private String html;

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public ScoreLine[] getScoreLines() {
		return scoreLines;
	}

	public void setScoreLines(ScoreLine[] scoreLines) {
		this.scoreLines = scoreLines;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

}
