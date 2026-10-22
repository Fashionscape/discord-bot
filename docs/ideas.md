# Ideas to imlpement eventually

This is a combination of both things that I want to do, and features that were removed from production in favor of easier v3 streamlining.

### Member Role
**What:** Give a new user a Member role a set time after joining the server. This member role gives that user the ability to talk in channels. The role and the timeout should be set by Administrators via bot commands. Enabled and configured on a per-server basis.
**Why:** We often have to shield the server against bot accounts, trolls, or spammers. Not being able to talk in any channel upon joining filters out the vast majority of these accounts. 

### Achievement Listener
**What:** A channel-based automated reaction engine that adds a celebratory reaction to people's posts in a staff-determined achievements channel. Achievement channel and reaction emoji should be set by Administrators via bot commands. Enabled and configured on a per-server basis.
**Why:** To make it easier for others to celebrate and react to achievements. Foster camaraderie.

### New Member Listener / Welcome Message Engine
**What:** A listener that tracks new joiners to the server. It then spits out a targeted welcome message to that user in a dedicated channel. The message and the channel should be set by Administrators. Enabled and configured on a per-server basis. Used and implemented in conjunction with the Member Role Listener
**Why:** Help new members orient themselves and prevents spammers. Can be used to provide a more seamless onboarding experience.

### Report Listener / Moderation Reporting
**What:** A listener that tracks messages in a dedicated reporting channel. It keeps the channel clear and forwards all reports to the moderation team, in a dedicated moderator-only channel. The reporting channel and the moderator channel should be set by Administrators. Enabled and configured on a per-server basis.
**Why:** Helps moderation by allowing users to reach the entire mod team with one message safely.

### Thumbs Listener / Announcements / News / Votes
**What:** A configurable listener that provides social media-like pre-set reactions to messages. Designed to be used in announcement or similar style channels. Enabled and configured on a per-server basis by the Administrators, should be able to handle up to 5 (or more?) channels and react to each message with a thumbs up/down.
**Why:** Helps start reactions, especially in scenarios where all server members (including staff) have their votes counted.