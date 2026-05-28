package com.tp1.habittracker.repository.graph;

import com.tp1.habittracker.domain.graph.UserNode;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

public interface UserGraphRepository extends Neo4jRepository<UserNode, String> {

    @Query("MERGE (u:User {id: $userId}) RETURN u")
    UserNode ensureUserNode(@Param("userId") String userId);

    @Query("MATCH (u:User {id: $userId}) DETACH DELETE u")
    void deleteUserNode(@Param("userId") String userId);

    @Query("""
            MATCH (u:User {id: $userId})
            MATCH (h:Habit {id: $habitId})
            MERGE (u)-[:HAS_HABIT]->(h)
            """)
    void linkHabit(@Param("userId") String userId, @Param("habitId") String habitId);

    @Query("""
            MATCH (u:User {id: $userId})-[r:HAS_HABIT]->(h:Habit {id: $habitId})
            DELETE r
            """)
    void unlinkHabit(@Param("userId") String userId, @Param("habitId") String habitId);

    @Query("""
            MATCH (requester:User {id: $requesterId})
            MATCH (target:User {id: $targetId})
            WHERE NOT (requester)-[:FRIENDS_WITH]-(target)
            MERGE (requester)-[r:FRIEND_REQUEST]->(target)
              ON CREATE SET r.createdAt = datetime()
            """)
    void createFriendRequest(@Param("requesterId") String requesterId,
                             @Param("targetId") String targetId);

    @Query("""
            MATCH (requester:User {id: $requesterId})-[r:FRIEND_REQUEST]->(target:User {id: $targetId})
            DELETE r
            WITH requester, target
            MERGE (a:User {id: CASE WHEN $requesterId < $targetId THEN $requesterId ELSE $targetId END})
            MERGE (b:User {id: CASE WHEN $requesterId < $targetId THEN $targetId ELSE $requesterId END})
            MERGE (a)-[f:FRIENDS_WITH]->(b)
              ON CREATE SET f.since = datetime()
            """)
    void acceptFriendRequest(@Param("requesterId") String requesterId,
                             @Param("targetId") String targetId);

    @Query("""
            MATCH (requester:User {id: $requesterId})-[r:FRIEND_REQUEST]->(target:User {id: $targetId})
            DELETE r
            """)
    void deleteFriendRequest(@Param("requesterId") String requesterId,
                             @Param("targetId") String targetId);

    @Query("""
            MATCH (a:User {id: $userAId})-[r:FRIENDS_WITH]-(b:User {id: $userBId})
            DELETE r
            """)
    void removeFriendship(@Param("userAId") String userAId,
                          @Param("userBId") String userBId);

    /**
     * Idempotent direct-create of a friendship edge, bypassing request/accept. Used by the
     * startup seeder. Stores the edge in canonical direction (smaller id → larger id) so a
     * subsequent {@code -[:FRIENDS_WITH]-} query finds it regardless of input order.
     */
    @Query("""
            MERGE (a:User {id: CASE WHEN $userAId < $userBId THEN $userAId ELSE $userBId END})
            MERGE (b:User {id: CASE WHEN $userAId < $userBId THEN $userBId ELSE $userAId END})
            MERGE (a)-[f:FRIENDS_WITH]->(b)
              ON CREATE SET f.since = datetime()
            """)
    void createFriendship(@Param("userAId") String userAId,
                          @Param("userBId") String userBId);

    /**
     * Habit-id candidates owned by friends (distance 1) or friends-of-friends (distance 2),
     * excluding habits the user already owns. Returns id + minimum graph distance for
     * downstream similarity scoring and weighting.
     */
    @Query("""
            MATCH (me:User {id: $userId})
            MATCH path = (me)-[:FRIENDS_WITH*1..2]-(other:User)
            WHERE other.id <> $userId
            WITH me, other, min(length(path)) AS distance
            MATCH (other)-[:HAS_HABIT]->(h:Habit)
            WHERE NOT (me)-[:HAS_HABIT]->(h)
            WITH h, min(distance) AS distance
            RETURN h.id AS habitId, distance
            ORDER BY distance ASC, h.id ASC
            LIMIT $limit
            """)
    List<RecommendationCandidate> findRecommendationCandidates(@Param("userId") String userId,
                                                                @Param("limit") int limit);

    @Query("""
            MATCH (me:User {id: $userId})-[:FRIENDS_WITH]-(friend:User)
            RETURN friend.id
            """)
    List<String> findFriendIds(@Param("userId") String userId);

    @Query("""
            MATCH (me:User {id: $userId})<-[:FRIEND_REQUEST]-(requester:User)
            RETURN requester.id
            """)
    List<String> findIncomingRequestIds(@Param("userId") String userId);

    @Query("""
            MATCH (me:User {id: $userId})-[:FRIEND_REQUEST]->(target:User)
            RETURN target.id
            """)
    List<String> findOutgoingRequestIds(@Param("userId") String userId);
}
