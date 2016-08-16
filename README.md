[![Build Status](https://img.shields.io/travis/jqno/secretaresse.svg?style=plastic)](https://travis-ci.org/jqno/secretaresse)

# Secretaresse

This tool is your personal secretary: she keeps your Google Calendar in sync with your Exchange calendar!

# Getting started

You need to set up a couple of things before you can run this code.

* You need to create a new calendar in your Google Calendar. Don't forget to do this; Secretaresse WILL wipe everything that's in there (if it's not also in Exchange). Don't say I didn't warn you! In Google Calendar, click in the arrow box next to "My calendars" and click "Create a new calendar".

* You need an API key. (You didn't think I'd give you mine, did you? ;).) Instructions for how to get one are [here](https://developers.google.com/google-apps/calendar/quickstart/java). Follow the instructions in Step 1. Save the credentials to a file called `client_secret.json` in the root folder of the project.

* You need to create a file called `application.conf` in the root folder of the project. It needs to look like this:

```
  exchange {
    userName = "your.emailaddress@employer.com"
    password = "sup3r s3cr3t p4ssw0rd g03s h3r3"
  }

  google {
    calendarName = "the name of the calendar you just created"
  }
```

* Did you remember to create a new Google Calendar? You'll lose your data if you don't! There is no undo.

* Run the program with `sbt run`.

### Periodically run Secretaresse with Hammerspoon
To run Secretaresse periodically, simply include the hammerspoon `secretaresse.lua` to your `init.lua` script and tell it where your project lives.
```lua
secretaresseDir = '../workspace/secretaresse'
dofile("../secretaresse/secretaresse.lua")

-- Or this way
secretaresseDir = '~/workspace/secretaresse'
dofile(<ABSOLUTE-PATH> .. "/secretaresse/secretaresse.lua")


-- Or place secretaresse.lua in your hammerspoon dir and import it with:
secretaresseDir = '~/workspace/secretaresse'
require('secretaresse')
```
