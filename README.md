# Goombotio

This is a Twitch bot for use in JCog's channel with the Goombotio account.

## Commands

Goombotio allows moderators to add, remove, modify, and see details for custom commands.

#### Adding Commands

##### Usage

>!addcom `command_name` "`command response`"

`command_name` is the name of the command you wish to add. This typically begins with an exclamation point but is not required to.

`command response` is the message you want Goombotio to reply with when the command is called. It must be surrounded with quotation marks. There is no need to escape any quotation marks contained in the response.

##### Example

>!addcom !testing "this is a test message"

When `!testing` is called, the command will return:

>this is a test message

#### Editing Commands

##### Usage

>!editcom `command_name` "`command response`"

`command_name` is the name of the command you wish to edit

`command response` is the message you want Goombotio to reply with when the command is called. It must be surrounded with quotation marks. There is no need to escape any quotation marks contained in the response.

##### Example

>!editcom !testing "new test message"

When `!testing` is called, the command will return:

>new test message

#### Deleting Commands

##### Usage

>!delcom `command_name`

`command_name` is the name of the command you wish to delete

##### Example

>!delcom !testing

#### Details

##### Usage

>!comdetails `command_name`

`command_name` is the name of the command you wish to see full details for

##### Example

>!comdetails !testing

#### Advanced Usage

##### User Levels and Cooldowns

When adding and editing commands, you can also specify the minimum user level required to execute the command, as well as the command's cooldown time. To do so, add one or both of the arguments below. If adding a new command or editing the message of an old one, the arguments can go before or after the message.

`-c`, `--cooldown` the minimum number of seconds between command uses.

`-l`, `--userlevel` the minimum user level required to use the command, as specified below

##### User Levels

1. **broadcaster**: streamer
2. **mod**: channel moderator
3. **vip**: channel VIP
4. **staff**: Twitch staff
5. **sub**: channel subscriber
6. **default**: anyone

##### Examples

>!addcom !testing --cooldown 10 --userlevel vip "test message"

>!addcom !testing "test message" -c 5

>!editcom !testing -l mod "new test message"

>!editcom !testing --cooldown 7 --userlevel default

#### Variables

Variables (and nested variables) can be used within command responses. All variables are of the form:

$(`variable` `argument`)

|               variable | description                                                                                                                                                                 |
|-----------------------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `arg 0`, `arg 1`, etc. | arguments for user input after the command, split by spaces. arguments are zero-indexed. not to be confused with variable arguments.                                        |
|              `channel` | the streamer's username                                                                                                                                                     |
|               `choose` | randomly selects a message from a <code>&#124;</code> separated list in `argument`                                                                                          |
|              `command` | returns the result of the command from `argument`. properties such as `count`, userlevel, and cooldown will come from the parent command.                                   |
|                `count` | the number of times this command has been called. does not increment unless this variable is used                                                                           |
|                 `eval` | evaluates mathamatical expressions. for example, the output of `$(eval pi^2/e)` would be `3.63`                                                                             |
|            `followage` | the length of time the user in `argument` has been following the channel                                                                                                    |
|                `query` | full user input that comes after the command                                                                                                                                |
|                 `rand` | returns a random integer. `argument` should be a comma-separated, inclusive range                                                                                           |
|               `touser` | returns the same as `arg 0`, but if there are no user arguments, defaults to the user's username. if `arg 0` starts with '@', returns the substring of `arg 0` without it.  |
|               `uptime` | the length of time the stream has been online                                                                                                                               |
|             `urlfetch` | output from a remote url                                                                                                                                                    |
|                 `user` | the twitch username of the user                                                                                                                                             |
|               `userid` | the twitch userId of the user                                                                                                                                               |
|             `weighted` | randomly selects a message from a <code>&#124;</code> separated list in `argument`. each message should start with a positive weight, followed by a space, then the message |

## Scheduled Messages

The !scheduled command allows moderators to add, remove, and modify scheduled messages. Every 20 minutes, as long as
there has been chat activity, one message from the pool of messages is posted. The same message will never be posted
twice in a row.

#### Adding Scheduled Messages

##### Usage

!scheduled add `message_id` "`message response`"

`!message_id` is the name of the message you wish to add

`message response` is the message you want Goombotio to have a chance of displaying. It must be surrounded with
quotation marks. There is no need to escape any quotation marks contained in the response.

##### Example

!scheduled add testing "this is a test message"

If this message is selected, Goombotio will display:

this is a test message

#### Editing Scheduled Messages

##### Usage

!scheduled edit `message_id` "`message response`"

`message_id` is the name of the message you wish to edit

`message response` is the message you want Goombotio to have a chance of displaying. It must be surrounded with
quotation marks. There is no need to escape any quotation marks contained in the response.

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

Goombotio can keep store and respond with quotes it has been given. Only mods can add/edit/delete quotes, but anyone can
call !quote and !latestquote.

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

Deletes the given quote. If there are quotes with higher numbers than this one, each one has its value decremented by
one.

#### Undo/Redo

`!undoquote` and `!redoquote`

This works exactly how you'd expect. If you add, edit, or delete a quote, `!undoquote` will undo that action so that
everything will be exactly like it was before that action. `!redoquote` will redo anything that has been undone. Up to
10 actions are kept in memory to be undone/redone.