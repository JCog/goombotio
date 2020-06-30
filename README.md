# Goombotio
This is a Twitch bot for use in JCog's channel with the Goombotio account.

## Commands
The !commands command allows moderators to add, remove, modify, and see details for custom commands. It's use is similar to how Nightbot works, but not exactly the same.

#### Adding Commands
##### Usage
!commands add `!command_name` "`command response`"

`!command_name` is the name of the command you wish to add and must begin with an exclamation mark

`command response` is the message you want Goombotio to reply with when the command is called. It must be surrounded with quotation marks. There is no need to escape any quotation marks contained in the response.

##### Example
!commands add !testing "this is a test message"

When !testing is called, the command will return:

this is a test message

#### Editing Commands
##### Usage
!commands edit `!command_name` "`command response`"

`!command_name` is the name of the command you wish to edit

`command response` is the message you want Goombotio to reply with when the command is called. It must be surrounded with quotation marks. There is no need to escape any quotation marks contained in the response.

##### Example
!commands edit !testing "new test message"

When !testing is called, the command will return:

new test message

#### Deleting Commands
##### Usage
!commands delete `!command_name`

`!command_name` is the name of the command you wish to delete

##### Example
!commands delete !testing

#### Details
##### Usage
!commands details `!command_name`

`!command_name` is the name of the command you wish to see full details for

##### Example
!commands details !testing

#### Advanced Usage
##### User Levels and Cooldowns
When adding and editing commands, you can also specify the minimum user level required to execute the command, as well as the command's cooldown time. You just need to apply the parameters as shown below:

!commands add `!command_name` -ul=`userlevel` -cd=`cooldown` "`command response`"

`!command_name` / `command response` is the same as above.

`cooldown` is the minimum number of miliseconds between command uses.

`userlevel` is the minimum user level required to use the command, as specified below

##### User Levels
1. **broadcaster**: streamer
2. **mod**: channel moderator
3. **vip**: channel VIP
4. **staff**: Twitch staff
5. **sub**: channel subscriber
6. **default**: anyone

#### Variables
Variables (and nested variables) can be used within command responses. All variables are of the form:

$(`variable` `argument`)

* `arg 0`, `arg 1`, etc. - arguments for user input after the command, split by spaces. arguments are zero-indexed. not to be confused with variable arguments.
* `channel` - the streamer's username
* `count` - the number of times this command has been called. does not increment unless this variable is used
* `eval` - executes arbitrary javascript. be very careful not to call something with an infinite loop
* `followage` - the length of time the user in `argument` has been following the channel
* `query` - full user input that comes after the command
* `rand` - returns a random integer. `argument` should be a comma-separated, inclusive range
* `touser` - returns the same as `arg 0`, but if there are no user arguments, defaults to the user's username
* `uptime` - the length of time the stream has been online
* `urlfetch` - output from a remote url
* `userid` - the twitch userId of the user
* `weight` - randomly selects a message from a `|` separated list in `argument`. Each message should start with a positive weight, followed by a space, then the message

## Scheduled Messages
The !scheduled command allows moderators to add, remove, and modify scheduled messages. Every 20 minutes, as long as there has been active chat, one message from the pool of messages is posted. The same message will never be posted twice in a row.

#### Adding Scheduled Messages
##### Usage
!scheduled add `message_id` "`message response`"

`!message_id` is the name of the message you wish to add

`message response` is the message you want Goombotio to have a chance of displaying. It must be surrounded with quotation marks. There is no need to escape any quotation marks contained in the response.

##### Example
!scheduled add testing "this is a test message"

If this message is selected, Goombotio will display:

this is a test message

#### Editing Scheduled Messages
##### Usage
!scheduled edit `message_id` "`message response`"

`message_id` is the name of the message you wish to edit

`message response` is the message you want Goombotio to have a chance of displaying. It must be surrounded with quotation marks. There is no need to escape any quotation marks contained in the response.

##### Example
!scheduled edit testing "new test message"

If this message is selected, Goombotio will display:

new test message

#### Deleting Scheduled Messages
##### Usage
!scheduled delete `message_id`

`message_id` is the name of the message you wish to delete

##### Example
!scheduled delete testing

## Quotes
Goombotio can keep store and respond with quotes it has been given. Only mods can add/edit/delete quotes, but anyone can call !quote and !latestquote.

#### Getting Quotes
!quote `quote_number`

Responds with the quote with the given number. If `quote_number` is not specified, picks a random quote.

!latestquote

Responds with the most recently added quote.

#### Adding Quotes
!addquote "`quote text`"

Adds a quote to the quote database. Note that there is no need to add the date at the end as this is done automatically.

#### Editing Quotes
!editquote `quote_number` `new quote text`

Edits the text of the given quote.

#### Deleting Quotes
!delquote `quote_number`

Deletes the given quote. If there are quotes with higher numbers than this one, each one has its value decremented by one.