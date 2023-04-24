package chronocache.util;

import chronocache.core.qry.QueryIdentifier;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UndirectedGraph
{
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    Map<QueryIdentifier, List<QueryIdentifier>> adjacencyList;

    public UndirectedGraph()
    {
        adjacencyList = new HashMap<QueryIdentifier, List<QueryIdentifier>>();
    }

    public List<QueryIdentifier> get(QueryIdentifier key)
    {
        return adjacencyList.get(key);
    }

    public Set<QueryIdentifier> keySet()
    {
        return adjacencyList.keySet();
    }

    public void add(QueryIdentifier key, List<QueryIdentifier> edgeSet)
    {
        adjacencyList.put(key, edgeSet);
    }

    public void add(QueryIdentifier key, QueryIdentifier edge)
    {
        adjacencyList.get(key).add(edge);
    }


    // Clique detection helper function
    private Set<QueryIdentifier> findDisconnected(Set<QueryIdentifier> not, Set<QueryIdentifier> candidates)
    {
        // A set to allow us to iterate over oldNot U oldCandidates
        Set<QueryIdentifier> all = new HashSet<QueryIdentifier>();
        all.addAll(not);
        all.addAll(candidates);

        // Variables and sets to help determine the maximally connected vertex in candidates or not
        int minimumDisconnectionCount = Integer.MAX_VALUE;
        Set<QueryIdentifier> disconnected = new HashSet<QueryIdentifier>();

        // Look for a new maximally connected/minimally disconnected vertex
        for (QueryIdentifier queryId : all)
        {
            // Temporary set to keep count, won't be saved unless it's the best so far
            Set<QueryIdentifier> tempDisconnected = new HashSet<QueryIdentifier>();

            // Record disconnections
            for (QueryIdentifier candidate : candidates)
            {
                if (!adjacencyList.get(queryId).contains(candidate))
                {
                    tempDisconnected.add(candidate);
                }
            }

            // Test the new minimum
            if (tempDisconnected.size() < minimumDisconnectionCount)
            {
                // Save the set of disconnected nodes so it can be returned
                minimumDisconnectionCount = tempDisconnected.size();
                // Clear out the old, bigger set
                disconnected.clear();
                // Copy over temp to our permanent set
                disconnected.addAll(tempDisconnected);

                // We will move the node in candidate over to not before considering other candidates
                if (candidates.contains(queryId))
                {
                    disconnected.add(queryId);
                }
            }
        }

        return disconnected;
    }

    // Clique detection helper function
    private Set<Set<QueryIdentifier>> extend(Set<QueryIdentifier> computedSubgraph, Set<QueryIdentifier> oldNot, Set<QueryIdentifier> oldCandidates)
    {
        // The set of cliques we will return
        Set<Set<QueryIdentifier>> finalUnion = new HashSet<Set<QueryIdentifier>>();

        // Find the vertices which are disconnected from the maximally connected vertex
        // will sometimes contain the vertex itself if it was from oldCandidates
        Set<QueryIdentifier> disconnected = findDisconnected(oldNot, oldCandidates);

        // Branch and bound cycle
        for (QueryIdentifier disconnectedQueryId : disconnected)
        {
            // New sets which will be filled based on the graph, oldNot, and oldCandidates
            Set<QueryIdentifier> newNot = new HashSet<QueryIdentifier>();
            Set<QueryIdentifier> newCandidates = new HashSet<QueryIdentifier>();

            // Fill the set newNot
            for (QueryIdentifier notQueryId : oldNot)
            {
                if (notQueryId != disconnectedQueryId && adjacencyList.get(disconnectedQueryId).contains(notQueryId))
                {
                    newNot.add(notQueryId);
                }
            }

            // Fill the set newCandidates
            for (QueryIdentifier candidateQueryId : oldCandidates)
            {
                if (candidateQueryId != disconnectedQueryId && adjacencyList.get(disconnectedQueryId).contains(candidateQueryId))
                {
                    newCandidates.add(candidateQueryId);
                }
            }

            // Add the considered vertex to computedSubgraph
            computedSubgraph.add(disconnectedQueryId);

            if (newNot.isEmpty() && newCandidates.isEmpty())
            {
                // Add the subgraph as a clique, adding 1-cliques only if there is really a self-edge in the graph
                if (computedSubgraph.size() > 1 || adjacencyList.get(disconnectedQueryId).contains(disconnectedQueryId))
                {
                    finalUnion.add(new HashSet<QueryIdentifier>(computedSubgraph));
                }
            }
            else if (!newCandidates.isEmpty())
            {
                // Recurse
                finalUnion.addAll(extend(computedSubgraph, newNot, newCandidates));
            }

            // Remove the considered vertex from computedSubgraph
            computedSubgraph.remove(disconnectedQueryId);

            // Shift the considered vertex from oldCandidates set to oldNot
            oldNot.add(disconnectedQueryId);
            oldCandidates.remove(disconnectedQueryId);
        }

        return finalUnion;
    }

    // Clique detection using Bron et. al's method
    public Set<Set<QueryIdentifier>> detectCliques()
    {
        // Setup initially empty sets
        Set<QueryIdentifier> oldNot = new HashSet<QueryIdentifier>();
        Set<QueryIdentifier> computedSubgraph = new HashSet<QueryIdentifier>();

        // Deep copy the keyset of the graph making all nodes candidates initially
        Set<QueryIdentifier> oldCandidates = new HashSet<QueryIdentifier>();
        oldCandidates.addAll(adjacencyList.keySet());

        // Start the recursive function with its initial values
        return extend(computedSubgraph, oldNot, oldCandidates);
    }

    // Very slow method to check if a node is in a clique. Does NP hard clique detection every time
    public boolean inClique(QueryIdentifier queryId)
    {
        // Retrieve cliques
        Set<Set<QueryIdentifier>> cliques = detectCliques();

        // Check each clique to see if queryId is in it
        for (Set<QueryIdentifier> clique : cliques)
        {
            if (clique.contains(queryId))
            {
                return true;
            }
        }

        return false;
    }

    // Utility function for debugging
    public void print()
    {
        for (QueryIdentifier queryId : adjacencyList.keySet())
        {
            logger.trace("Q{} -> [", queryId.getId());
            for (QueryIdentifier secondaryQueryId : adjacencyList.get(queryId))
            {
                logger.trace("    Q{}", secondaryQueryId.getId());
            }
            logger.trace("]");
        }
    }

    // Utility function for debugging
    public void printCliques()
    {
        // Retrieve cliques
        Set<Set<QueryIdentifier>> cliques = detectCliques();

        // Print cliques one by one
        for (Set<QueryIdentifier> clique : cliques)
        {
            logger.trace("Printing clique of size {}", clique.size());
            for (QueryIdentifier queryId : clique)
            {
                logger.trace("    {}", queryId.getId());
            }
        }
    }
}
