# Developer FAQ #

**How should one handle repository being unavailable during init()?**

It is OK for init() to error if repository is unavailable, as the
GsaCommunicationHandler will retry init() using exponential backoff
(starting at some seconds and maxing out at an hour) until it is interrupted, shutdown, or successful.


**How do I get a reference to the DocIdPusher?**

Use AdaptorContext.getDocIdPusher() that you get in init.


**How do I feed document that has changed?**

Send a Record for that DocId with crawl-immediately being true.


**How do I feed a document that has been deleted?**

Tell the GSA to crawl the document, which will result in 404, which will result in a deletion.  If you'd like you can set delete to be true in Record, but that's not necessary.


**How do I make an ACL?**

Use Acl.Builder

**I'm getting interruption exception, what am I doing wrong?**

The adaptor has 30 seconds to start sending content, and 3 minutes to complete sending content.  The config values are adaptor.docContentTimeoutSecs and adaptor.docHeaderTimeoutSecs.

**My adaptor freezes!**

Windows users need JRE 1.7u6 or higher (earlier versions freeze) .

**My adaptor has a memory leak!**

All users need at least JRE 1.6u27 or higher (earlier versions have a memory leak).

**I'm seeing the same config name appear twice in dashboard?**

Please double check that you don't have a Unicode invisible space character, that maybe was copied from a webpage or was inserted at the beginning of the file by your text editor, in your config name.

**I have an existing Adaptor that provides full data pushes - how do I make it support incremental pushes?**

Implement `com/google/enterprise/adaptor/PollingIncrementalLister` which has a single method `getModifiedDocIds(DocIdPusher pusher)` . When initializing the Adaptor, register with `AdaptorContext.setPollingIncrementalLister(yourLister)`.

**How do I check if GSA is giving if-modified-since?**

[Request.hasChangedSinceLastAccess()](http://hourly.plexi.googlecode.com/git/javadoc/com/google/enterprise/adaptor/Request.html#hasChangedSinceLastAccess(java.util.Date))

**How do I give last-modified time to the GSA?**

[Response.setLastModified()](http://hourly.plexi.googlecode.com/git/javadoc/com/google/enterprise/adaptor/Response.html#setLastModified(java.util.Date))

**How do I give back a verified identify when writing my own authenticateUser method?**

Use the callback passed to [authenticateUser()](http://documentation.plexi.googlecode.com/git-history/v4.0.1/javadoc/com/google/enterprise/adaptor/AuthnAuthority.Callback.html#userAuthenticated(com.sun.net.httpserver.HttpExchange,%20com.google.enterprise.adaptor.AuthnIdentity)) .  This callback will result in sending verified identity information to the GSA via SAML (the SAML is handled by the library). Note that the 2nd argument of this callback is an AuthnIdentity.  The AuthnIdentity is where you can provide the user name and groups which have been verified. Here is an example of an AuthnIdentity that you can provide to the callback:
```
      AuthnIdentity identity = new AuthnIdentity() {

        @Override
        public UserPrincipal getUser() {
          return user;
        }

        @Override
        public String getPassword() {
          return null;
        }

        @Override
        public Set<GroupPrincipal> getGroups() {
          return groups;
        }
      };
```