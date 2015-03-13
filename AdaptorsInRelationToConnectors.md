# The Origin #

Adaptors effectively make a repository look like an HTTP server. The thought process behind adaptors is that the GSA is great at crawling HTTP servers and discovering modified content, so let's make the repository's content available via HTTP. Since documents in repositories aren't very interrelated (they have few hyperlinks), just crawling would only find a small percentage of the content. So periodically the adaptor provides a list of documents for the GSA to crawl (for example, say once a day). The GSA still gets to decide when to crawl each document. Re-sending a document id to the GSA is effectively a no-op on the GSA, so the adaptor doesn't even need to keep track of what it sent to the GSA previously. With just a document lister and a document retriever you can get all the documents into the GSA and not need to keep any state on the Adaptor.

Since some repositories make it easy to notice what has changed recently, adaptors for those repositories can inform the GSA immediately when things happen, and even force the GSA to immediately recrawl a document because the adaptor knows that document was modified recently. This can allow the GSA to pick up changes to a repository with very low latency, but is not required for correct operation.

# Connector Concerns #

This process is different from previous connectors. Connectors push all data and do not allow the GSA to re-query data as needed. This means that
  1. the connector must have its own crawler to notice updates,
  1. the connector needs to keep track of the GSA’s state and the repository’s state, and
  1. if an error occurs on the GSA (due to load or some other intermittent issue) then there is no avenue for the GSA to re-obtain the file for indexing.

Each of these three items causes an issue.

Item 1 means duplication of crawler-related code, configuration, and maintenance. This is made more severe by the GSA’s crawler having many diagnostics available but the connector’s crawler having few. There is no reasonable way for a user to understand the current state of a connector.

Item 2 requires noticing updates to the repository, but also being able to handle failures from the GSA. Many connectors need to keep large amounts of state to correctly notice updates. This led to an initiative to provide large, high-availability data stores for use with connectors. That means that there can be three high-availability data stores in the system: the repository, the connector, and the GSA. To handle failures, the connector manager has a checkpoint system that requires connectors to be able to save and restore checkpoints in case of errors. For some repositories, these requirements are easy to deal with, however, for others they have required considerable amount of code. If any point in the repository’s history cannot be easily referenced (i.e., there isn’t a single identifier across the entire repository) this becomes a major problem.

Item 3 causes errors and bugs to be persistent. When an intermittent error occurs during indexing the GSA will not index the file. The only way to correct the error is by restarting a full, overwriting push of repository and hoping the second time fairs better. File system connector tests have shown that for document sets over 100,000, it is common to have a missing document.

This is not meant to dis connectors. Instead, it is meant to define the problems to be solved. The connector design is highly reasonable and does prove to work, but we want connectors to be easy to write and use, and the listed items slow development and comprehension.

# The Lister/Retriever Model #

The lister/retriever architecture of adaptors provides many advantages over using content feeds when you can randomly access content: simplicity, scalability, self-healing, statelessness, transparent operation (easy monitoring and debugging), and it reuses existing infrastructure.

The original expectation was that adaptors would have a slight performance loss when using only a single GSA. Since adaptors have the ability to scale to multiple GSAs, it was thought to likely be worth the performance penalty in the single-box setup. However, testing revealed that adaptors can actually be faster than connectors when the repository is not the bottleneck. For a file system repository containing no documents requiring text conversion and where latency could have had an impact, the adaptor performed 20% better than the corresponding connector. This in no way means there will always be a performance gain, but instead merely that it is possible and has happened.

The only known performance concern at this point is repository access ordering. Some repositories perform better if the document contents are retrieved in a particular order. A connector may be able to satisfy the requirements of checkpointing and still be able to retrieve documents in the preferred order. Adaptors would not be able to change the retrieval order. The main time this performance characteristic would be an issue is during initial crawling of a large corpus.

However, this initial push performance should not be as large of a concern for adaptors as it is for connectors. Initial crawling with connectors is a more common action than it is for adaptors since restarting a connector is the only way of dealing with indexing errors. In addition, connectors can only have the GSA index the content linearly from the start of the repository’s history. Adaptors are able to have the GSA index new and updated content at the same time as historic content. Adaptors are even able to give higher indexing priority to the fresh content over stale content. This means that the user does not need to wait for the initial push to complete before the indexing provided by the GSA can prove useful.