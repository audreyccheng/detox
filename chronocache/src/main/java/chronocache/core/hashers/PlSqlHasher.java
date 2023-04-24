package chronocache.core.hashers;

import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.parser.AntlrListener;
import chronocache.core.parser.PlSqlTypes;

/**
 * This class recurses down the parse tree and hashes each leaf node.
 * The hash of a leaf node depends on whether we consider a node to be a constant,
 * constant nodes receive the same hash value. This allows us to have the same
 * hash values for queries which only differ in the value of constants in the
 * The ignored hash is the hash of all contents within a qsh_ignore function
 */
public class PlSqlHasher extends AntlrListener {
    private long hash;
    private long ignoredHash;
    // Some large prime out of range of other constants ot avoid hash collisions
    private static int CONSTANT_HASH_VAL = 131311;
    private Logger log = LoggerFactory.getLogger(this.getClass());

    public PlSqlHasher() {
        super();
        hash = 0;
        ignoredHash = 0;
    }

    public long getHash() {
        return hash;
    }


    /**
	 * Updates the hash by or-ing with the objectKey, and left shifting the hash
	 * @param objectKey
	 */
    private void updateHash(int objectKey) {
        hash ^= objectKey;
        hash = Long.rotateLeft(hash, 7);
    }

    /**
     * Updates the hash with a string of text.
     * we hash the lower case of strings to ensure that identifiers which are
     * capitalized differently are the same. This means table names must be
     * distinct irrespective of case.
	 * @param text
     */
    private void updateHash(String text) {
        String lowerText = text.toLowerCase();
        updateHash(lowerText.hashCode());
        log.trace("updating hash with {} to {}", lowerText, hash);
    }

    /**
     * Hashes a constant leaf by first updating the hash with the type of the node,
     * and then updating the hash with a constant
	 * @param node
     */
    private void hashConstantLeaf(TerminalNode node) {
        log.trace("hashing leaf {} as constant", node.getText());
        PlSqlTypes type = super.peekContext();
        log.trace("hashing type as well {}", type);
        updateHash(type.name());
        updateHash(CONSTANT_HASH_VAL);
    }


    /**
     * Hashes a leaf node, either with its value or with a constant representation
     * of it
	 * @param node
     */
    private void hashLeafNode(TerminalNode node) {
        if (super.shouldConsiderLeafConstant()) {
            hashConstantLeaf(node);
        } else if (super.isNodeAdditive(node)) {
            log.trace("not hashing additive leaf", node.getText());
        } else {
            updateHash(node.getText());
        }
    }

    /**
     * Visits every node, and updates the hash with it if it is a leaf
	 * @param node
     */
	@Override public void visitTerminal(@NotNull TerminalNode node) {
        if (node.getChildCount() == 0) {
            log.trace("leaf node: {}", node.getText());
            hashLeafNode(node);
        }
    }

}
