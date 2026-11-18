const pptxgen = require("pptxgenjs");
const path = require("node:path");
const ppt = new pptxgen();
ppt.layout = "LAYOUT_WIDE";
ppt.author = "Campus Lost and Found project team";
ppt.title = "Campus Lost and Found System";
ppt.theme = {headFontFace:"Aptos Display",bodyFontFace:"Aptos",lang:"en-IN"};
const green = "00635F", ink = "172935", muted = "49636E", mint = "EEF6F6", line = "CBDDDF";
const images = process.argv[2];
const out = process.argv[3];
function t(s, text, x, y, w, h, opts={}) {
  s.addText(text,{x,y,w,h,fontFace:"Aptos",fontSize:22,color:ink,margin:0,breakLine:false,fit:"shrink",...opts});
}
let slideNumber = 1;
function base(title) {
  const s=ppt.addSlide();
  s.background={color:"FFFFFF"};
  t(s,title,0.72,0.52,11.9,0.62,{fontSize:32,bold:true,color:green});
  s.addShape(ppt.ShapeType.line,{x:.72,y:1.31,w:11.89,h:0,line:{color:line,width:1}});
  t(s,"CAMPUS LOST AND FOUND SYSTEM",.72,7.08,11,.19,{fontSize:10,color:muted});
  t(s,String(++slideNumber).padStart(2,"0"),11.95,7.08,.65,.19,{fontSize:10,color:muted,align:"right"});
  return s;
}
function list(title, entries, notes) {
  const s=base(title);
  entries.forEach(([heading,body],i)=>{
    const y=1.65+i*1.22;
    t(s,heading,.76,y,11.7,.33,{bold:true,fontSize:21,color:green});
    t(s,body,.76,y+.4,11.7,.55,{fontSize:20});
  });
  s.addNotes([notes]);
  return s;
}
function shot(title, file, heading, body, notes) {
  const s=base(title);
  const image=path.join(images,file+".png");
  s.addImage({path:image,x:.75,y:1.52,w:8.2,h:5.18});
  t(s,heading,9.3,2.0,3.25,1.0,{bold:true,fontSize:26,color:green});
  t(s,body,9.3,3.13,3.25,2.3,{fontSize:20,valign:"top"});
  t(s,"Actual application render with fictional data",.75,6.77,8.2,.23,{fontSize:12,color:muted});
  s.addNotes([notes]);
}
{
 const s=ppt.addSlide();s.background={color:mint};
 t(s,"JAVA 17  /  PBL EXHIBITION",.76,.86,11,.34,{fontSize:15,bold:true,color:green});
 t(s,"Campus Lost and\nFound System",.72,1.85,11.9,2.24,{fontFace:"Aptos Display",fontSize:58,bold:true,color:ink});
 t(s,"Item reports, verified claims and recorded handovers",.78,4.58,11.6,.57,{fontSize:25,color:green});
 t(s,"[College]   •   [Four approved team members]",.78,6.48,11.6,.35,{fontSize:15,color:muted});
 s.addNotes(["Introduce the four approved team members. Explain that the desk workflow uses a local Java application."]);
}
list("Problem and context",[
 ["Scattered item reports","Lost and found information can be spread across registers, desks and chat messages."],
 ["Slow lookup","A reasoned search needs fields such as item type, location, date and category."],
 ["Incomplete handover records","Campus staff need to record who claimed an item and when it was returned."]
],"Explain a real campus example without identifying a student.");
list("Project objectives",[
 ["Capture","Record lost and found items with relevant details and an optional photo."],
 ["Find","Combine a keyword search with type, category and status filters."],
 ["Verify and return","Record an ownership detail and complete the handover with a timestamp."]
],"Staff perform the verification. The software records their decision.");
{
 const s=base("Application architecture");
 const steps=[["Swing","Forms and report board"],["CampusService","Validation and workflow"],["Repository","JDBC persistence"],["SQLite","Reports and claims"]];
 steps.forEach(([h,b],i)=>{
  let x=.74+3.05*i;
  s.addShape(ppt.ShapeType.rect,{x,y:2.29,w:2.72,h:1.31,line:{color:line},fill:{color:mint}});
  t(s,h,x+.14,2.53,2.43,.3,{bold:true,fontSize:19,color:green});
  t(s,b,x+.14,3.0,2.43,.3,{fontSize:14});
  if(i<3)t(s,"→",x+2.76,2.69,.26,.27,{fontSize:17,color:green});
 });
 t(s,"The service depends on the ItemRepository interface. SqliteRepository implements it.",.76,4.53,11.6,1.0,{fontSize:23});
 s.addNotes(["Trace a New report action through Swing, the service, repository and database."]);
}
list("Java concepts",[
 ["Records and enums","Item and Draft encapsulate data. Enums define the legal kinds, categories and statuses."],
 ["Interfaces and polymorphism","CampusService calls ItemRepository. The SQLite implementation handles storage."],
 ["Swing and JDBC","Inheritance, event listeners, prepared statements, exceptions and try-with-resources."]
],"Point to CampusFrame, Item, CampusService and SqliteRepository during the viva.");
shot("Working demo: file a report","02-report-form","New found report","Enter the item name, category, location, date and a distinguishing description. A JPEG or PNG is optional.","The demo uses a fictional blue calculator report.");
shot("Working demo: find and claim","04-filtered","Search and verify","Search for calculator. Select the matching report and record the claimant and an ownership detail.","The results show exactly one matching report in the capture. The claim is only available on an open found item.");
shot("Working demo: confirm return","07-returned","Recorded handover","The item status becomes Returned. The detail panel retains the claim and return timestamps.","The same SQLite file was reopened after the UI scenario to verify the return.");
list("Database design",[
 ["Items","Primary key, report type, description, date, category, location, optional BLOB and status."],
 ["Claims","item_id references the item and allows a single claim. The row keeps evidence and timestamps."],
 ["Transaction rule","A status change and its matching claim write commit together or roll back together."]
],"A guarded SQL update prevents competing claims from succeeding twice.");
list("Test results",[
 ["12 integration tests passed","Coverage includes input, filters, photo persistence, state transitions and rollback."],
 ["Executable JAR check","CI launched Java 17 and opened a seeded SQLite database from the shaded JAR."],
 ["Swing walkthrough passed","The scenario filed, located, claimed and returned an item, then reopened the database."]
],"The public repository's Actions tab contains the build logs.");
list("Four-member presentation",[
 ["Student 1","Introduce the problem and file the sample report."],
 ["Student 2","Show filtering and explain records and enums."],
 ["Student 3","Record the claim and explain JDBC and transactions."],
 ["Student 4","Confirm the return and explain tests and persistence."]
],"Replace suggested roles with actual contributions and bring the updated practical lab file.");
list("Conclusion and future scope",[
 ["Working Java application","A campus desk can store reports, retrieve records and document a verified handover."],
 ["Current scope","The application runs at one local desk and requires staff verification of the claim."],
 ["Possible extensions","Operator sign-in, shared desks and closing linked lost reports."]
],"Open the packaged demo or play the recorded walkthrough if a live desktop is unavailable.");
ppt.writeFile({fileName:path.join(out,"Campus_Lost_and_Found_Presentation.pptx")});
