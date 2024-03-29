package testplayer;

import battlecode.common.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

public class Politician extends RobotPlayer{
    static int[] ECIDs = new int[20];
    static MapLocation[] ECLocations = new MapLocation[20];
    static int currentHomeEC = -1;
    static int ecsStored = -1;
    static final int SCOUTING = 0;
    static final int ATTACKING = 1;
    static final int CONVERTED = 2;
    static final int FOLLOW = 3;
    //static final int OVERFLOW = 4;
    static int role;
    static MapLocation target;
    static int[] homeECFlagContents;
    static int homeECx;
    static int homeECy;
    static int muckrakersInRange;
    static int trailedID;
    static int scoutedEnemyMuckID;
    static Direction scoutDir;

    static void setup() throws GameActionException {
        turnCount = rc.getRoundNum();
        RobotInfo[] possibleECs = rc.senseNearbyRobots(2, rc.getTeam());
        for (int i = possibleECs.length; --i >= 0;) {
            if (possibleECs[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                currentHomeEC ++;
                ECIDs[currentHomeEC] = possibleECs[i].getID();
                ECLocations[currentHomeEC] = possibleECs[i].getLocation();
                ecsStored ++;
            }
        }
        if (currentHomeEC == -1) {
            role = CONVERTED;
            rc.setFlag(0);
        } else {
            homeECx = ECLocations[currentHomeEC].x;
            homeECy = ECLocations[currentHomeEC].y;
            rc.setFlag(0);
            scoutDir = awayFromLocation(ECLocations[currentHomeEC]);
            role = SCOUTING;
            /*
            if (rc.getEmpowerFactor(rc.getTeam(), 10) > 2 && rc.getConviction() > 1000) {
                rc.setFlag(0);
                role = OVERFLOW;
            } */
        }
    }

    static void run() throws GameActionException {
        if (currentHomeEC == -1) {
            role = CONVERTED;
        }
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        int distToHome = 0;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] enemiesInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, enemy);
        muckrakersInRange = 0;
        RobotInfo[] friendlyInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
        if (role != CONVERTED) {
            if (rc.canGetFlag(ECIDs[currentHomeEC])) {
                homeECFlagContents = decodeFlag(rc.getFlag(ECIDs[currentHomeEC]));
                distToHome = rc.getLocation().distanceSquaredTo(ECLocations[currentHomeEC]);
            } //maybe something about if cant then its taken
        }
        if (homeECFlagContents == null) {
            role = CONVERTED;
            currentHomeEC = -1;
        }
        //poli v poli micro
        //follow muckrakers if big enough poli
        for (int i = enemiesInRange.length; --i >= 0;) {
            if (enemiesInRange[i].getType() == RobotType.POLITICIAN && rc.getConviction() > enemiesInRange[i].getConviction() + 10
                    && rc.getConviction() < 1000 && (enemiesInRange[i].getConviction() > rc.getConviction() - 100) && nearECS(196)) {
                RobotInfo unit = enemiesInRange[i];
                if (trailedID == 0 && decodeFlag(rc.getFlag(rc.getID()))[0] != NEUTRAL_EC_FOUND && (turnCount > 20 || rc.getConviction() < 100)) {
                    trailedID = unit.getID();
                    role = FOLLOW;
                    break;
                }
            }
            else if (enemiesInRange[i].getType() == RobotType.MUCKRAKER && (rc.getConviction() < enemiesInRange[i].getConviction() + 30
                    || (nearECS(144) && (turnCount > 300 || rc.getConviction() < 100)))
                    && (rc.getConviction() > enemiesInRange[i].getConviction() + 10 || (enemiesInRange[i].getConviction() >= 255 && rc.getConviction() > 100))
                && ECLocations[0] != null && nearECS(500)) {
                RobotInfo unit = enemiesInRange[i];
                if (trailedID == 0 && notTrailed(unit.getID(), friendlyInRange)
                        && decodeFlag(rc.getFlag(rc.getID()))[0] != NEUTRAL_EC_FOUND) {
                    trailedID = unit.getID();
                    role = FOLLOW;
                    rc.setFlag(encodeFlag(0, unit.getLocation().x - homeECx, unit.getLocation().y-homeECy, Math.min(255, unit.getConviction())));
                    break;
                }
            }
        }
        for (int i = attackable.length; --i >= 0;) {
            if (attackable[i].getType() == RobotType.MUCKRAKER) {
                muckrakersInRange ++;
            }
        }
        /*if (role == OVERFLOW) {
            if (rc.canEmpower(2)) {
                rc.empower(2);
            }
        } else */
        if (role == SCOUTING) {
            //go to point along direction of creation
            RobotInfo[] unitsInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
            if (!rc.canSenseRobot(scoutedEnemyMuckID)) {
                rc.setFlag(0);
            }
            for (int i = enemiesInRange.length; --i >= 0;) {
                RobotInfo unit = enemiesInRange[i];
                if (unit.getType() == RobotType.MUCKRAKER) {
                    rc.setFlag(encodeFlag(0, unit.getLocation().x - homeECx, unit.getLocation().y - homeECy, Math.min(unit.getConviction(), 255)));
                    scoutedEnemyMuckID = unit.getID();
                    break;
                }
            }
            for (int i = unitsInRange.length; --i >= 0;) {
                RobotInfo unit = unitsInRange[i];
                if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == enemy) {
                    rc.setFlag(encodeFlag(ENEMY_EC_FOUND, unit.location.x - homeECx, unit.location.y - homeECy, Math.min(unit.getConviction(), 255)));
                    break;
                }
                else if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == Team.NEUTRAL) {
                    rc.setFlag(encodeFlag(NEUTRAL_EC_FOUND, unit.location.x - homeECx, unit.location.y - homeECy, Math.min(unit.getConviction(), 255)));
                    break;
                }
            }
            if (homeECFlagContents != null) {
                MapLocation tgtedEC = new MapLocation(homeECx + homeECFlagContents[1], homeECy + homeECFlagContents[2]);
                if (rc.canSenseLocation(tgtedEC)) {
                    RobotInfo tgt = rc.senseRobotAtLocation(tgtedEC);
                    if (tgt.getType() == RobotType.ENLIGHTENMENT_CENTER && tgt.getTeam() == rc.getTeam()
                            && tgt.getLocation().equals(tgtedEC)) {
                        role = SCOUTING;
                        rc.setFlag(encodeFlag(SECURED_EC, tgtedEC.x - homeECx, tgtedEC.y - homeECy, 0));
                    }
                }
            }
            if ((rc.getID() % 3 == 0) && rc.getInfluence() < 30) {
                tryMove(polisringv2());
            }
            else if (shouldSpread()) {
                tryMove(getPathDirSpread());
            } else if (rc.getID() % 2 == 0){
                tryMove(polisringv2());
            } else {
                tryMove(scoutDir);
                if (!rc.onTheMap(rc.getLocation().add(scoutDir))) {
                    scoutDir = scoutDir.opposite();
                }
            }
        } else if (role == ATTACKING) {
            //only attacks target location atm, no reaction to other units on the way
            if (rc.canSenseLocation(target)) {
                RobotInfo tgt = rc.senseRobotAtLocation(target);
                if (tgt.getType() == RobotType.ENLIGHTENMENT_CENTER && tgt.getTeam() == rc.getTeam()
                        && tgt.getLocation().equals(target)) {
                    role = SCOUTING;
                    rc.setFlag(encodeFlag(SECURED_EC, target.x - homeECx, target.y - homeECy, 0));
                }
            }
            if (rc.getLocation().distanceSquaredTo(target) < actionRadius
                    && (rc.getConviction() - 10)*rc.getEmpowerFactor(rc.getTeam(), 0) > 0
            && rc.canEmpower(rc.getLocation().distanceSquaredTo(target)) && role == ATTACKING) {
                if (rc.getLocation().distanceSquaredTo(target) == 1
                        || (rc.getCooldownTurns() < 1 && !rc.canMove(rc.getLocation().directionTo(target))
                        && movesSinceClosest > 4)) {
                    rc.empower(rc.getLocation().distanceSquaredTo(target));
                } else if (canKill(rc.senseRobotAtLocation(target), rc.getLocation().distanceSquaredTo(target))) {
                    rc.empower(rc.getLocation().distanceSquaredTo(target));
                }
            }
            if ((rc.getID() % 3 == 0) && rc.getInfluence() < 30) {
                tryMove(polisringv2());
            }
            else {
                tryMove(getPathDirTo(target));
            }
        } else if (role == CONVERTED) { //right now they just run around and kamikaze
            if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
            }

            if (shouldSpread()) {
                tryMove(getPathDirSpread());
            } else {
                tryMove(randomDirection());
            }

        } else if (role == FOLLOW) {
            if (rc.canSenseRobot(trailedID) && notTrailed(trailedID, friendlyInRange)) {
                RobotInfo trailed = rc.senseRobot(trailedID);
                if (trailed.getType() == RobotType.POLITICIAN) {
                    target = trailed.getLocation();
                    if (rc.canEmpower(rc.getLocation().distanceSquaredTo(trailed.getLocation()))
                            && canKill(trailed, rc.getLocation().distanceSquaredTo(trailed.getLocation()))) {
                        rc.empower(rc.getLocation().distanceSquaredTo(trailed.getLocation()));
                    }
                    tryMove(getPathDirTo(target));
                }
                else if (trailed.getType() == RobotType.MUCKRAKER) {
                    target = trailed.getLocation();
                    rc.setFlag(encodeFlag(0, trailed.getLocation().x - homeECx,
                            trailed.getLocation().y - homeECy, Math.min(trailed.getConviction(), 255)));
                    if (ECLocations[0] != null
                            && nearECS(196)
                            && rc.canEmpower(rc.getLocation().distanceSquaredTo(trailed.getLocation()))
                            && canKill(trailed, rc.getLocation().distanceSquaredTo(trailed.getLocation()))) {
                        rc.empower(rc.getLocation().distanceSquaredTo(trailed.getLocation()));
                    } else if (muckrakersInRange > 1 && rc.canEmpower(actionRadius) && rc.getConviction() < 30) {
                        ArrayList<RobotInfo> mucksInAttackRange = new ArrayList<>();
                        for (int i = attackable.length; --i >= 0;) {
                            if (attackable[i].getType() == RobotType.MUCKRAKER) {
                                mucksInAttackRange.add(attackable[i]);
                            }
                        }
                        int highestCanKill = 0;
                        int optimalExplodeRadius = actionRadius;
                        for (int i = 1; i <= 9; i ++) {
                            int canKillCount = 0;
                            for (RobotInfo muck : mucksInAttackRange) {
                                if (canKill(muck, i)) {
                                    canKillCount ++;
                                }
                            }
                            if (canKillCount > highestCanKill) {
                                highestCanKill = canKillCount;
                                optimalExplodeRadius = i;
                            }
                        }
                        if (highestCanKill >= 2) {
                            rc.empower(optimalExplodeRadius);
                        }
                    } else if (ECLocations[0] != null && !nearECS(500)) {
                        trailedID = 0;
                        rc.setFlag(0);
                        role = SCOUTING;
                    }
                    tryMove(getPathDirTo(target));
                }
                else {
                    trailedID = 0;
                    rc.setFlag(0);
                    role = SCOUTING;
                }
            } else {
                trailedID = 0;
                rc.setFlag(0);
                role = SCOUTING;
            }
        }

        //reading home ec flag info
        if (homeECFlagContents != null) {
            //if its an attack command, attack
            int[] ownFlag = decodeFlag(rc.getFlag(rc.getID()));
            if (((homeECFlagContents[0] == ENEMY_EC_FOUND &&
                rc.getConviction() > 29) || (homeECFlagContents[0] == NEUTRAL_EC_FOUND
                    && ((homeECFlagContents[3] == 255 && rc.getConviction() > 510 && rc.getConviction() < 610)
                    || (homeECFlagContents[3] < 255 && rc.getConviction() > homeECFlagContents[3] + 10
                    && rc.getConviction() < homeECFlagContents[3] + 110))))
                && role != FOLLOW && ownFlag[0] != SECURED_EC) {
                rc.setFlag(rc.getFlag(ECIDs[currentHomeEC]));
                target = new MapLocation(homeECx + homeECFlagContents[1],
                        homeECy + homeECFlagContents[2]);
                role = ATTACKING;
            } else if (homeECFlagContents[0] == SECURED_EC && ownFlag[1] == homeECFlagContents[1]
                    && ownFlag[2] == homeECFlagContents[2]){
                rc.setFlag(0);
                role = SCOUTING;
            }
        }

        for (int i = friendlyInRange.length; --i >= 0; ) {
            if (friendlyInRange[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                if (currentHomeEC == -1) {
                    currentHomeEC ++;
                    ECIDs[currentHomeEC] = friendlyInRange[i].getID();
                    ECLocations[currentHomeEC] = friendlyInRange[i].getLocation();
                    homeECx = ECLocations[currentHomeEC].x;
                    homeECy = ECLocations[currentHomeEC].y;
                    rc.setFlag(0);
                    scoutDir = awayFromLocation(ECLocations[currentHomeEC]);
                    role = SCOUTING;
                } else if (!contains(friendlyInRange[i].getID(), ECIDs)){
                    ecsStored ++;
                    ECIDs[ecsStored] = friendlyInRange[i].getID();
                    ECLocations[ecsStored] = friendlyInRange[i].getLocation();
                }
            }
        }
    homeECFlagContents = null;
    }

    static boolean canKill(RobotInfo tgt, int radius) {
        int unitsInRange = rc.senseNearbyRobots(radius).length;
        return (((rc.getConviction()- 10)*rc.getEmpowerFactor(rc.getTeam(), 0))/unitsInRange > tgt.getConviction());
    }

    static boolean nearECS(int radius) {
        for (int i = 0; i < ecsStored + 1; i ++) {
            if (rc.getLocation().distanceSquaredTo(ECLocations[i]) < radius) {
                return true;
            }
        }
        return false;
    }

    static boolean notTrailed(int trailedMuckrakerID, RobotInfo[] friendlies) throws GameActionException {
        if (rc.canSenseRobot(trailedMuckrakerID)) {
            RobotInfo tgt = rc.senseRobot(trailedMuckrakerID);
            MapLocation tgtLoc = tgt.getLocation();
            for (int i = friendlies.length; --i >= 0;) {
                if (friendlies[i].getType() == RobotType.POLITICIAN && friendlies[i].getConviction() > tgt.getConviction() + 10) {
                    if (friendlies[i].getLocation().distanceSquaredTo(tgtLoc)
                            < rc.getLocation().distanceSquaredTo(tgtLoc)) {
                        int[] friendlyFlag = decodeFlag(rc.getFlag(friendlies[i].getID()));
                        if (friendlyFlag[0] == 0 && friendlyFlag[1] == tgtLoc.x && friendlyFlag[2] == tgtLoc.y) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    static boolean contains(int ecid, int[] arr) {
        for (int i = arr.length; --i >= 0;) {
            if (arr[i] == ecid) {
                return true;
            }
        }
        return false;
    }

    static Direction getPathDirSpread() throws GameActionException {
        Team friendly = rc.getTeam();
        RobotInfo[] friendlies = rc.senseNearbyRobots(25, friendly);
        ArrayList<RobotInfo> nearbyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyecs.add(robot);
            }
        }
        int numberofnearbyfriendlies = friendlies.length;

        numberofnearbyfriendlies = friendlies.length > 10 ? 10 : numberofnearbyfriendlies; // cap at 10

        Direction optimalDir = Direction.CENTER;
        double optimalCost = - Double.MAX_VALUE;
        for (Direction dir: directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj) && rc.canMove(dir)) {
                double pass = rc.sensePassability(adj);
                double cost = - (rc.getType().actionCooldown/pass);
                /*
                if (nearbyecs.size() != 0) {
                    MapLocation spreadfromecone = nearbyecs.get(0).getLocation();
                    cost += (Math.pow(spreadfromecone.x - adj.x, 2) + Math.pow(spreadfromecone.y - adj.y, 2));
                }

                 */
                for(int i = numberofnearbyfriendlies; --i>=0;)
                {
                    MapLocation spreadFrom = friendlies[i].getLocation();
                    cost += (25.0/rc.getLocation().distanceSquaredTo(spreadFrom)) * ((Math.abs(adj.x - spreadFrom.x) + Math.abs(adj.y - spreadFrom.y))
                            - (Math.abs(rc.getLocation().x - spreadFrom.x) + Math.abs(rc.getLocation().y - spreadFrom.y)));
                  /*  if (role != CONVERTED) {
                        cost += (Math.abs(adj.x - homeECx) - Math.abs(rc.getLocation().x - homeECx) +
                                Math.abs(adj.y - homeECy) - Math.abs(rc.getLocation().y - homeECy));
                    }*/
                }
                if (cost > optimalCost && rc.canMove(dir)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        return optimalDir;
    }

    static Direction polisringv2() throws GameActionException {
        MapLocation home = ECLocations[currentHomeEC];
        Direction optimalDir = Direction.CENTER;
        double optimalCost = - Double.MAX_VALUE;

        if (turnCount % 10 == 0) {
            banList.clear();
        }
        for (Direction dir: directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj) && rc.canMove(dir)) {
                double pass = rc.sensePassability(adj);
                //double cost = - (rc.getType().actionCooldown/pass);
                double cost = 0;
                double radius = 6.2;
                cost -= Math.abs(radius - Math.sqrt(Math.pow(home.x - adj.x, 2) + Math.pow(home.y - adj.y, 2)));
                cost += radius/6 * pass;
                if (cost > optimalCost && rc.canMove(dir) && !banList.contains(adj)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        return optimalDir;
    }
}
