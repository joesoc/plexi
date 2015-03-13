Adaptors are a solution for providing your repository data to your Google Search Appliance (GSA). If you are already familiar with Connectors and the GSA, then you may be interested in the AdaptorsInRelationToConnectors.

# Introduction: Content Repositories and the GSA #

For each repository you want to search with your GSA, you need some way to provide that repository's data to your GSA. If your repository provides an HTTP website, then no special solution should be necessary, as the GSA should be able to crawl your repository like any other website. However, sometimes you want closer integration with the GSA or your repository doesn't provide a website, then you can write an adaptor.

There are several ways to model and communicate your repository's contents to the GSA, and Adaptors are one of them. For other possible solutions, look into [Connectors](http://code.google.com/p/googlesearchapplianceconnectors/) and [Content Feeds](http://code.google.com/apis/searchappliance/documentation/614/feedsguide.html). Adaptors are attempting to replace the Connector model while being easier to learn and develop, but Connectors are the current standard and support older GSA versions. Content Feeds should be used when the repository does not provide  random document access and instead _only_ provides changes occurring in the repository.

All Adaptors use the Adaptor Library to communicate with the GSA. The Adaptor Library defines much of the architecture of the Adaptor and controls communication with the GSA. Since all Adaptors share the Adaptor Library, the rest of the discussion will relate to the Adaptor Library and what it provides to Adaptors.

# Adaptor Library's Objectives #

Shallow learning curve, incremental development, security and performance are the core design forces of the Adaptor Library. We want you to be able to create a working Adaptor within a day, incrementally add extra features, and still be able to tune for high performance.

The APIs are designed so you can ignore pieces you don't need, and only learn them once you need them. This makes the various features separate and orthogonal to each other. To [implement a basic Adaptor](https://code.google.com/p/plexi/source/browse/src/com/google/enterprise/adaptor/examples/AdaptorTemplate.java), you only need to implement two methods and know the basics of five different classes. In some ways, five classes may seem like too many, but they all serve an obvious function and you only need one method from each.

Performance is a concern for the Adaptor Library, but it does come second to ease of use. However, the library has been able to achieve high performance and handle millions of documents without giving up ease of development; we generally expect the GSA or the repository to be the bottleneck.

In order to make correct Adaptors easy to create, we have pushed to make them stateless (or mostly stateless). Individual Adaptors can still store state if necessary, but we discourage it. When stateless, Adaptors are resilient, use little memory, are easier to code, and are easy to use in a high availability solution. When optimizing for performance, we find it reasonable to use a minimal amount of state in memory (like a date or revision number), but the system is designed to work even if such state is lost or corrupted.

# Adaptor Architecture #

Adaptors have two main pieces and a third piece for non-public repositories. The two main pieces are a _lister_ and a _retriever_. With just those two pieces the core architecture is formed and a working Adaptor can be created. The third piece performs authorization.

The _retriever_ is an HTTP server that responds to requests for particular documents. When the GSA would like the contents of a document, it sends a request to the retriever and downloads the contents. Documents are identified by a DocId, and it is the retriever's job to get the bytes for the document referenced by a DocId.

The _lister_ informs the GSA that documents exist in the repository. Commonly it will list all the documents in the repository. However, with some Adaptors it doesn't need to list all the documents because the GSA can find the rest through its normal crawling. Once the GSA has been informed about a document via the lister, it will then call the retriever to download the contents.

The authorization piece is provided a list of DocIds and a user, and is given the task of determining which DocIds the the user can access. This is used when the GSA is configured to use it (it is possible to have security in place without it) or when a user points his browser to the HTTP server manually to access the document.