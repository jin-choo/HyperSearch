mvn compile -B

mvn exec:java -Dexec.args="citeseer"
mvn exec:java -Dexec.args="cora"
mvn exec:java -Dexec.args="coraA"
mvn exec:java -Dexec.args="dblpA"

mvn exec:java -Dexec.args="email-Enron"
mvn exec:java -Dexec.args="email-Eu"
mvn exec:java -Dexec.args="contact-high-school"
mvn exec:java -Dexec.args="contact-primary-school"
mvn exec:java -Dexec.args="tags-ask-ubuntu"
mvn exec:java -Dexec.args="tags-math-sx"
