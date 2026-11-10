# YEP web prototype

A small Java web app for the Youth Empowerment Project. It uses the JDK's built-in HTTP server, plain HTML/CSS/JavaScript, and the existing YEP Java domain classes. There are no frameworks or external libraries to install.

## Run it

1. Install JDK 17 or later.
2. Run `run.bat` on Windows, or compile and start it from this folder:

   ```text
   javac -d out src/yep/*.java
   java -cp out yep.WebServer
   ```

3. Open [http://localhost:8080](http://localhost:8080).

Demo participant: `amina@mail.com` / `pass1234`

## What you can explore

- Public YEP landing page and programme overview
- Participant login and account creation with consent and age checks
- Learning dashboard and three short interactive learning guides
- Level Two team, mentor, project and milestone view
- Editable participant portfolio with skills and visibility settings
- Certificate verification

The prototype stores its sample data and sessions in memory. New accounts, profile edits, lesson progress and milestones reset when the server stops. It is a local classroom prototype, not a production service.
