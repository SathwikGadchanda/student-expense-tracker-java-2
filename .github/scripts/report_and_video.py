from pathlib import Path
from xml.sax.saxutils import escape
from PIL import Image, ImageDraw, ImageFont
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Image as PdfImage, Paragraph, PageBreak, SimpleDocTemplate, Spacer, Table, TableStyle
from reportlab.graphics.shapes import Drawing, Line, Polygon, Rect, String
import subprocess
import sys

shots, out = map(Path, sys.argv[1:3])
out.mkdir(exist_ok=True)
ink = colors.HexColor("#172935")
green = colors.HexColor("#00635F")
muted = colors.HexColor("#49636E")
mint = colors.HexColor("#EEF6F6")
rule = colors.HexColor("#CBDDDF")
styles = {
    "title": ParagraphStyle("title", fontName="Helvetica-Bold", fontSize=35, leading=41, textColor=ink, spaceAfter=20),
    "h": ParagraphStyle("h", fontName="Helvetica-Bold", fontSize=17, leading=21, textColor=green, spaceBefore=14, spaceAfter=8, keepWithNext=True),
    "p": ParagraphStyle("p", fontName="Helvetica", fontSize=11, leading=16, textColor=ink, spaceAfter=9),
    "small": ParagraphStyle("small", fontName="Helvetica", fontSize=9, leading=13, textColor=muted, spaceAfter=8),
}
story = []

def p(value, style="p"):
    return Paragraph(escape(value), styles[style])

def section(n, heading, *paragraphs):
    story.append(p(f"{n:02d}  {heading}", "h"))
    story.extend(p(t) for t in paragraphs)

def page():
    story.append(PageBreak())

def table(entries, widths):
    t = Table([[p(value, "small") for value in row] for row in entries], colWidths=widths, hAlign="LEFT", repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0,0), (-1,0), mint), ("VALIGN", (0,0), (-1,-1), "TOP"),
        ("LEFTPADDING", (0,0), (-1,-1), 8), ("RIGHTPADDING", (0,0), (-1,-1), 8),
        ("TOPPADDING", (0,0), (-1,-1), 7), ("BOTTOMPADDING", (0,0), (-1,-1), 5),
        ("LINEBELOW", (0,0), (-1,-1), .4, rule),
    ]))
    story.extend([t, Spacer(1,12)])

def diagram(labels):
    d = Drawing(488, 58)
    w = 104
    for index, label in enumerate(labels):
        x = index*127
        d.add(Rect(x,10,w,36,rx=4,ry=4,fillColor=mint,strokeColor=rule))
        d.add(String(x+w/2,24,label,textAnchor="middle",fontName="Helvetica-Bold",fontSize=9,fillColor=green))
        if index < len(labels)-1:
            d.add(Line(x+w,28,x+119,28,strokeColor=green))
            d.add(Polygon([x+120,28,x+115,31,x+115,25],fillColor=green,strokeColor=green))
    story.append(d)

def picture(name, caption):
    file = shots / (name + ".png")
    with Image.open(file) as image:
        w, h = image.size
    story.append(PdfImage(str(file), width=490, height=490*h/w))
    story.extend([Spacer(1,8), p(caption, "small")])

def frame(c, d):
    width, height = d.pagesize
    c.setStrokeColor(rule)
    c.line(52, 47, width-52, 47)
    c.setFont("Helvetica", 8)
    c.setFillColor(muted)
    c.drawString(52, 32, "CAMPUS LOST AND FOUND SYSTEM   /   PBL PROJECT REPORT")
    c.drawRightString(width-52, 32, str(d.page))

story.extend([Spacer(1,78),p("PROJECT-BASED LEARNING / JAVA 17", "small"),Spacer(1,17),
              p("Campus Lost and Found System","title"),
              p("A desktop application for campus item reports, verified claims and recorded handovers."),
              Spacer(1,30)])
table([["Academic detail","Complete before submission"],["Institution","[College name]"],["Programme and section","[Programme / section]"],
       ["Course and year","[Course / academic year]"],["Student 1","[Name / roll number]"],["Student 2","[Name / roll number]"],
       ["Student 3","[Name / roll number]"],["Student 4","[Name / roll number]"],["Project guide","[Name, where applicable]"]],[175,315])
page()

section(2,"Certificate placeholder","Use the certificate issued or prescribed by your institution. This page has no signatures or institutional approval.")
table([["Fields","Institution to complete"],["Project","Campus Lost and Found System"],["Department","_________________________"],
       ["Guide and signature","_________________________"],["Department representative","_________________________"],
       ["Place and date","_________________________"]],[180,310])
section(3,"Acknowledgement","The team acknowledges the Java, SQLite and Maven reference documentation. Add acknowledgements for the people who actually contributed to or guided the project.")
section(4,"Abstract","Campus Lost and Found System brings lost and found reports into one local desktop application. Staff enter reports, search by text and category, and record ownership checks and physical handovers. Java 17 and Swing provide the desktop interface. SQLite keeps records between launches.","The repository interface separates data storage from the workflow. Guarded updates and transactions prevent duplicate claims and inconsistent status changes. Automated integration tests and a Swing walkthrough verify the implementation.")
page()

section(5,"Introduction","Students may leave umbrellas, calculators or notebooks in shared spaces. A campus desk needs to log each report, locate possible matches and keep a record of the handover.")
section(6,"Problem statement","Develop a Java application that stores campus item reports in one place and helps staff search, verify and document a return.")
section(7,"Objectives","Record item type, category, name, date, location and description. Accept an optional photo. Support combined search and filters. Record a claimant, the verification note and the time an item was returned.")
section(8,"Existing system","A paper ledger is quick to start but harder to search by several fields. Informal chat messages may spread information across groups and lack consistent return status. These are illustrative workflows for the problem statement.")
section(9,"Proposed system","A Swing board displays reports and status. The staff member enters a report, searches for it, records an ownership detail and confirms the handover. The database retains local history.")
section(10,"Software and hardware requirements","Java 17 or later, a graphical desktop and write access for SQLite. Maven is needed to build from source. A general-purpose laptop can run the app. An internet connection is needed to download Maven dependencies for the first build.")
section(11,"Technology used","Java 17, Swing, JDBC, SQLite, Maven, JUnit and GitHub. Java implements the entire application and its tests.")
page()

section(12,"System architecture","The desktop interface calls CampusService. The service uses an ItemRepository. SqliteRepository handles JDBC and supports one local database file.")
diagram(["Swing","CampusService","Repository","SQLite"])
section(13,"Use case diagram","The campus desk operator uses each of the following available actions.")
diagram(["Report item","Search records","Record claim","Confirm return"])
section(14,"Flowchart","The found report starts Open. Staff record verification before it can become Claimed. After handover, staff confirm the return and the report becomes Returned.")
diagram(["Open found item","Verify ownership","Claimed","Returned"])
section(15,"Database design","SQLite creates two tables automatically. claims.item_id is both the primary key and a foreign key to items.id.")
table([["Table","Important columns"],["items","id, kind, title, category, location, item_date, description, photo, status"],
       ["claims","item_id, claimant, evidence, recorded_at, returned_at"]],[100,390])
page()

section(16,"Implementation details","Item and Draft Java records model report data. Enums restrict the type, category and status. The normal path uses Swing event handling. A validation failure appears in the form rather than creating a partial record.","CampusService validates photo bytes and filters item lists. SqliteRepository binds user input using prepared statements and opens connections with try-with-resources. Claim and return updates use JDBC transactions and roll back on failure.","Photos are optional PNG or JPEG files up to 4 MB. The operating system controls access to the local database. This desk edition has no in-app sign-in and keeps data on one computer.")
section(17,"Application screenshots","The images below come from running the actual Swing application with a fictional calculator report.")
picture("04-filtered","Figure 1. Keyword search isolates the newly filed report.")
page()
picture("02-report-form","Figure 2. Filing a found-item report with the relevant location and description.")
picture("07-returned","Figure 3. Returned status and the saved verification and timestamps.")
page()

section(18,"Testing and results","The GitHub build ran Maven with Java 17. All 12 integration tests passed, the executable JAR opened SQLite with sample records, and the scripted Swing scenario exercised real forms and reopened the same database.")
table([["Verification","Outcome"],["Report creation and editing","Pass. Item IDs and edits were checked."],
       ["Combined filters","Pass. Text, type, category and status were combined."],["Photo handling","Pass. A valid JPEG or PNG is required and saved bytes persist."],
       ["State and return rules","Pass. The claim and return timestamps persisted."],["Rollback and concurrent claims","Pass. A failed write rolled back and only one competing claim won."],
       ["Swing walkthrough","Pass. Entered, searched, claimed and returned a report, then reopened SQLite."]],[185,305])
section(19,"Advantages","The board consolidates item information and reduces lookup time. The claim and return timestamps provide a traceable record. The packaged application can run offline.")
section(20,"Limitations","One local operator uses the app at a time. It supports a single claim for each found item. Staff decide whether evidence is sufficient. Lost reports remain reference records.")
section(21,"Future scope","Add multi-desk collaboration, operator sign-in, backups and a workflow for resolving linked lost reports.")
page()

section(22,"Conclusion","Campus Lost and Found System demonstrates Java application development from user interface to database. It provides a practical desk workflow for item reporting, search, ownership verification and recorded handovers.")
section(23,"References","Oracle Java SE 17 API and JDBC documentation: https://docs.oracle.com/en/java/javase/17/docs/api/",
        "SQLite documentation: https://www.sqlite.org/docs.html",
        "Apache Maven: https://maven.apache.org/guides/",
        "Project source and build evidence: https://github.com/SathwikGadchanda/student-expense-tracker-java-2")
story.append(p("Exhibition division for exactly four students", "h"))
table([["Student","Suggested demonstration contribution"],["1","Problem and report entry"],["2","Filters, records and enums"],
       ["3","Claim verification, JDBC and transactions"],["4","Return confirmation, results and persistence"]],[85,405])
story.append(p("Use the approved team list and record each member's actual contribution in the practical lab file."))
SimpleDocTemplate(str(out/"Campus_Lost_and_Found_Report.pdf"), pagesize=(595.28,841.89),
                  leftMargin=52,rightMargin=52,topMargin=52,bottomMargin=60,
                  title="Campus Lost and Found System - PBL Project Report").build(story,onFirstPage=frame,onLaterPages=frame)

scenes = [
    ("01-board", "Campus Lost and Found System", "Open the Java desktop app in demo mode to browse fictional campus reports."),
    ("02-report-form", "1. File a found-item report", "Enter the name, location and details. Attach a PNG or JPEG if available."),
    ("03-saved", "2. Save and review the report", "The calculator appears on the board with Open status."),
    ("04-filtered", "3. Search for the calculator", "The keyword narrows the list to the newly filed report."),
    ("05-claim-form", "4. Verify and record the claim", "Enter an ownership detail such as a scratch on the slide cover."),
    ("06-claimed", "5. Check the Claimed status", "The desk can now confirm the physical handover."),
    ("07-returned", "6. Confirm the return", "The result remains saved after reopening the SQLite database."),
]
font_dir = Path("/usr/share/fonts/truetype/dejavu")
heading = ImageFont.truetype(str(font_dir/"DejaVuSans-Bold.ttf"), 32)
caption = ImageFont.truetype(str(font_dir/"DejaVuSans.ttf"), 19)
video_frames = shots.parent / "frames"
video_frames.mkdir(exist_ok=True)
sequence = []
for number,(name,title,detail) in enumerate(scenes,1):
    image = Image.new("RGB",(1280,900),"#EEF6F6")
    draw = ImageDraw.Draw(image)
    draw.text((63,18),title,font=heading,fill="#00635F")
    draw.text((65,62),detail,font=caption,fill="#172935")
    with Image.open(shots/(name+".png")) as shot:
        shot.thumbnail((1180,765),Image.Resampling.LANCZOS)
        image.paste(shot,((1280-shot.width)//2,103))
    draw.text((63,863),f"Actual Swing application  /  Sample data  /  {number} of {len(scenes)}",font=caption,fill="#49636E")
    file = video_frames/f"scene-{number:02d}.png"
    image.save(file)
    sequence.extend([f"file '{file.name}'","duration 8"])
sequence.append(f"file 'scene-{len(scenes):02d}.png'")
manifest = video_frames/"sequence.txt"
manifest.write_text("\n".join(sequence)+"\n",encoding="utf-8")
subprocess.run(["ffmpeg","-hide_banner","-loglevel","error","-y","-f","concat","-safe","0","-i",str(manifest),
                "-c:v","libx264","-pix_fmt","yuv420p","-r","25","-movflags","+faststart",str(out/"Campus_Lost_and_Found_Demo.mp4")],check=True)
