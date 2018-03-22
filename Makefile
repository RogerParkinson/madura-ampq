build:
	mvn install

travis-deploy:
	gpg --import travis/private-key.gpg
	mvn versions:set -DnewVersion=${TRAVIS_TAG}
	mvn clean deploy -DskipTests=true -B -U -P release --settings travis/settings.xml