package org.khelekore.parjac2.javacompiler;

public enum ReturnState {
    RETURNS,      // returns
    SOFT_RETURN,  // returns, but we treat it softly, see handleIf
    THROWS,
    RETURNS_OR_THROWS,
    NO_RETURN;    // does not return or throw

    public ReturnState and (ReturnState other) {
	if (this == other)
	    return this;

	if (this == NO_RETURN || other == NO_RETURN)
	    return NO_RETURN;
	if (this == RETURNS_OR_THROWS || other == RETURNS_OR_THROWS)
	    return RETURNS_OR_THROWS;
	if (this == THROWS || other == THROWS)
	    return RETURNS_OR_THROWS;
	return SOFT_RETURN;
    }

    public ReturnState or (ReturnState other) {
	if (this == other)
	    return this;
	if (this == RETURNS_OR_THROWS || other == RETURNS_OR_THROWS)
	    return RETURNS_OR_THROWS;
	if (this == THROWS || other == THROWS)
	    return RETURNS_OR_THROWS;
	if (this == SOFT_RETURN || other == SOFT_RETURN)
	    return SOFT_RETURN;
	return RETURNS;
    }

    public boolean endsOrThrows () {
	return this == RETURNS || this == THROWS || this == RETURNS_OR_THROWS;
    }
}
