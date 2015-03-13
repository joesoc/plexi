# Sweeping guidelines #

  1. Don't introduce code provisionally. Only insert code that is being used for a purpose. Corollary: Develop using vertical slices, instead of horizontal slices.
  1. Each CL does one thing. Don't insert refactorings into functional changes and vice-versa.
  1. Aim for small CLs.
  1. Don't send out "try-things-out" CLs.
  1. Make CLs direct and concrete.
    * no unneccesary indirection; keep hierarchies shallow
    * put String constants directly in code instead of having unnecessary finals declared
  1. Prefer plain/idiomatic/obvious over clever.
  1. Name things!
    * make specific exception classes
    * don't use "abstract" in class name
    * don't use "impl" in class name
    * name sub-blocks of code by converting to methods


# Particular considerations #

  1. Is the code thread safe? Read your code and check whether your code behaves well when multiple methods of an instance are executing simultaneously. Avoid settors to make classes immutable.
  1. Was equals considered?
  1. Was hashCode considered?
  1. Were exceptions considered? Are exceptions scoped precisely and accurately?
  1. Was toString considered? toString is helpful for testing and debugging.
  1. Does the code call exit? Don't call exit as it makes code un-libifable and means shutdown sequenceing is not understood.
  1. Please provide sufficient tests.
  1. BufferedImage is TYPE\_INT\_RGB
  1. Gui use done with SwingUtilities.invokeLater () so that SwingUtilities.isEventDispatchThread()
  1. Usually if a class uses Random, it is parameterizable on Random
  1. Was use of System.out and System.err considered? Be very selective about putting information on these channels. Quite possibly never use them for communicating information.
  1. Watch out for autoboxing and varargs producing undesireable behaviour.
  1. Consider public and final modifiers on classes and methods.
  1. Add javadoc

# Detailed Google Java guide #

http://google-styleguide.googlecode.com/svn/trunk/javaguide.html