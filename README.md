# Google Search Test Automation 
This is a sample project created using page object model with TestNG as test framework.

Dependency
* Java
* Maven

###Libraries used
* Selenium
* TestNG
* WebdriverManager
* commons-io

### Google Input Tools -- A Chrome Plugin
The extension file is maintained in the src/main/resources/chromeExtension > **GoogleInputTool.crx** which will be used as a virtual keyboard.

Configuration is handled by extracting the id for the extension followed by enabling English keyboard.


### Steps to execute this project 

**Using Eclipse** :
Import the project to eclipse then, 
Goto Run > Run As > Maven Test 

**Using cmd** :
mvn clean test

