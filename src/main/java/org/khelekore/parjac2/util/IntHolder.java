package org.khelekore.parjac2.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class IntHolder {
    private final int blockSize;
    private final List<int[]> parts;
    private int currentSize;

    public IntHolder (int blockSize) {
	if (blockSize % 2 == 1)
	    throw new IllegalArgumentException ("Block size needs to be even");
	this.blockSize = blockSize;
	parts = new ArrayList<> ();
    }

    public int get (int i) {
	int blockId = i / blockSize;
	int[] block = parts.get (blockId);
	int posInBlock = i % blockSize;
	return block[posInBlock];
    }

    public void add (int i) {
	int positionInBlock = currentSize % blockSize;
	int[] block;
	if (positionInBlock == 0) {
	    parts.add (block = new int[blockSize]);
	} else {
	    int blockPosition = currentSize / blockSize;
	    block = parts.get (blockPosition);
	}
	block[positionInBlock] = i;
	currentSize++;
    }

    public void add (int i, int j) {
	int positionInBlock = currentSize % blockSize;
	int[] block;
	if (positionInBlock == 0) {
	    parts.add (block = new int[blockSize]);
	} else {
	    int blockPosition = currentSize / blockSize;
	    block = parts.get (blockPosition);
	}
	block[positionInBlock++] = i;
	block[positionInBlock] = j;
	currentSize += 2;
    }

    /** Give all the stored values from the given value up to, but not including to
     * @param function the function to run
     * @param from the start index
     * @param to the max index
     */
    public void apply (IntConsumer function, int from, int to) {
	validateRange (from, to);
	if (from == to)
	    return;
	int startBlock = from / blockSize;
	int startPos = from % blockSize;
	int endBlock = to / blockSize;
	int endPos = to % blockSize;
	int[] block = parts.get (startBlock);
	for (int c = from, posInBlock = startPos, currentBlock = startBlock; c < to; c++, posInBlock++) {
	    if (posInBlock == blockSize) {
		posInBlock = 0;
		currentBlock++;
		block = parts.get (currentBlock);
	    }
	    function.accept (block[posInBlock]);
	}
    }

    public void apply (IntIntConsumer function, int from, int to) {
	if (from % 2 == 1)
	    throw new IllegalArgumentException ("From-position is odd: " + from);
	validateRange (from, to);
	if (from == to)
	    return;
	int startBlock = from / blockSize;
	int startPos = from % blockSize;
	int endBlock = to / blockSize;
	int endPos = to % blockSize;
	int[] block = parts.get (startBlock);
	for (int c = from, posInBlock = startPos, currentBlock = startBlock; c < to; c += 2, posInBlock += 2) {
	    if (posInBlock == blockSize) {
		posInBlock = 0;
		currentBlock++;
		block = parts.get (currentBlock);
	    }
	    function.accept (block[posInBlock], block[posInBlock + 1]);
	}
    }

    private void validateRange (int from, int to) {
	if (from < 0)
	    throw new IllegalArgumentException ("from: " + from + " is < 0");
	if (to < 0)
	    throw new IllegalArgumentException ("to: " + to + " is < 0");
	if (to < from)
	    throw new IllegalArgumentException ("to: " + to + " is less than from: " + from);
	if (from > currentSize)
	    throw new ArrayIndexOutOfBoundsException ("From: " + from + " larger than current size: " +
						      currentSize);
	if (to > currentSize)
	    throw new ArrayIndexOutOfBoundsException ("To: " + to + " larger than current size: " +
						      currentSize);
    }

    public interface IntIntConsumer {
	void accept (int i, int j);
    }

    public int size () {
	return currentSize;
    }
}