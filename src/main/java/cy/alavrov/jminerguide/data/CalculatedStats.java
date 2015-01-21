/*
 * Copyright (c) 2015, Andrey Lavrov <lavroff@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package cy.alavrov.jminerguide.data;

import cy.alavrov.jminerguide.data.api.ship.HarvestUpgrade;
import cy.alavrov.jminerguide.data.api.ship.Hull;
import cy.alavrov.jminerguide.data.api.ship.OreType;
import cy.alavrov.jminerguide.data.api.ship.Ship;
import cy.alavrov.jminerguide.data.api.ship.Turret;
import cy.alavrov.jminerguide.data.character.EVECharacter;

/**
 * Stats, calculated for a ship, based on it's hull, modules, pilot and 
 * whatever else.
 *  
 * @author Andrey Lavrov <lavroff@gmail.com>
 */
public class CalculatedStats {
    /**
     * Yield of a turret, in m3.
     */
    private final float turretYield;
    
    /**
     * Yield of all turrets, combined, in m3.
     */
    private final float combinedTurretYield;
    
    /**
     * Cycle of a turret, in seconds.
     */
    private final float turretCycle;
    
    /**
     * Total turret yield per second, in m3/sec.
     */
    private final float turretM3S;
    
    /**
     * Yield of a drone, in m3.
     */
    private final float droneYield;
    
    /**
     * Yield of all drones, combined, in m3.
     */
    private final float combinedDroneYield;
    
    /**
     * Cycle of a drone, in seconds.
     */
    private final float droneCycle;
    
    /**
     * Total drone yield per second, in m3/sec.
     */
    private final float droneM3S;
    
    /**
     * Total ship yield, in m3/hour.
     */
    private final float totalM3H;
    
    /**
     * Ship's optimal, in metres.
     */
    private final int optimal;
    
    /**
     * Ship's ore (or cargo) hold, in m3.
     */
    private final int oreHold;
    
    /**
     * How long it takes for ore hold to fill, in seconds.
     */
    private final int secsForOreHold;
    
    public CalculatedStats(EVECharacter miner, Ship ship, boolean mercoxite) {

        
        Turret turret = ship.getTurret();
        Hull hull = ship.getHull();
        Hull.BonusCalculationResult bonus = hull.calculateSkillBonusModificators(miner);
        
        int upgrades = ship.getHarvestUpgradeCount();
        HarvestUpgrade upgrade = ship.getHarvestUpgrade();
        
        float baseTurretYield = turret.getBaseYield();
        float actualTurretYield;
        
        switch (turret.getTurretType()) {
            case MININGLASER:
            case STRIPMINER:
                actualTurretYield = baseTurretYield * 
                    (1 + hull.getRoleMiningYieldBonus()/100f) * 
                    bonus.miningYieldMod * miner.getMiningYieldModificator();
                
                if (upgrades > 0 ) {
                    for (int i = 0; i < upgrades; i++) {
                        actualTurretYield = actualTurretYield * 
                                (1 + upgrade.getOreYieldBonus() * 0.01f);
                    }
                }
                
                if (turret.isUsingCrystals()) {
                    if (turret.getOreType() == OreType.MERCOXIT && mercoxite) {
                        actualTurretYield = actualTurretYield * ship.getTurretCrystal().getMercMod();
                    } else {
                        actualTurretYield = actualTurretYield * ship.getTurretCrystal().getOreMod();
                    }
                }
                break;
                
            case GASHARVESTER:
            // only bonus yield for gas is from hulls. 
                actualTurretYield = baseTurretYield * 
                    (1 + hull.getRoleGasYieldBonus()/100f);
                break;
                
            default:
            case ICEHARVESTER:
                actualTurretYield = baseTurretYield;
                // ice harvesters have no bonus to yield.
                break;         
        }
        
        turretYield = actualTurretYield;
        combinedTurretYield = turretYield * ship.getTurretCount();
        
        float baseTurretCycle = turret.getCycleDuration();
        float actualTurretCycle;
        
        switch (turret.getTurretType()) {
            default:
            case MININGLASER:
                // booster to the rescue! later.                    
                actualTurretCycle = baseTurretCycle;
                break;
                
            case STRIPMINER:
                actualTurretCycle = baseTurretCycle * bonus.stripCycleMod;
                break;
                
            case GASHARVESTER:
                actualTurretCycle = baseTurretCycle * bonus.gasCycleMod * 
                    miner.getGasCycleBonus();
                break;
                
            case ICEHARVESTER:
                actualTurretCycle = baseTurretCycle * 
                    (1 - hull.getRoleIceCycleBonus()/100f) *
                    bonus.stripCycleMod * miner.getIceCycleBonus();
                
                if (upgrades > 0 ) {
                    for (int i = 0; i < upgrades; i++) {
                        actualTurretCycle = actualTurretCycle * 
                                (1 - upgrade.getIceCycleBonus()* 0.01f);
                    }
                }
                
                break;
        }
        
        turretCycle = actualTurretCycle;
        
        turretM3S = combinedTurretYield/turretCycle;
                
        
        // tba
        droneYield = 0; 
        combinedDroneYield = 0;
        droneCycle = 1;        
        
        droneM3S = combinedDroneYield / droneCycle;
        
        float totalM3S = turretM3S + droneM3S;
        totalM3H = totalM3S * 60 * 60;
        
        int baseOptimal = turret.getOptimalRange();
        int effectiveOptimal;
        
        switch (turret.getTurretType()) {
            case ICEHARVESTER:
            case STRIPMINER:
                effectiveOptimal = (int) (baseOptimal * bonus.stripOptimalMod);
                break;
                
            default:
                effectiveOptimal = baseOptimal;
        }
        
        // booster to the rescue, later.
        
        optimal = effectiveOptimal;
        
        int baseOreHold = hull.getOreHold();
        int effectiveOreHold = (int) (baseOreHold * bonus.oreHoldMod);
        
        oreHold = effectiveOreHold;
        
        secsForOreHold = (int) (oreHold / totalM3S);
    }

    /**
     * Yield of a turret, in m3.
     * @return the turretYield
     */
    public float getTurretYield() {
        return turretYield;
    }

    /**
     * Yield of all turrets, combined, in m3.
     * @return the combinedTurretYield
     */
    public float getCombinedTurretYield() {
        return combinedTurretYield;
    }

    /**
     * Cycle of a turret, in seconds.
     * @return the turretCycle
     */
    public float getTurretCycle() {
        return turretCycle;
    }

    /**
     * Total turret yield per second, in m3/sec.
     * @return the turretM3S
     */
    public float getTurretM3S() {
        return turretM3S;
    }

    /**
     * Yield of a drone, in m3.
     * @return the droneYield
     */
    public float getDroneYield() {
        return droneYield;
    }

    /**
     * Yield of all drones, combined, in m3.
     * @return the combinedDroneYield
     */
    public float getCombinedDroneYield() {
        return combinedDroneYield;
    }

    /**
     * Cycle of a drone, in seconds.
     * @return the droneCycle
     */
    public float getDroneCycle() {
        return droneCycle;
    }

    /**
     * Total drone yield per second, in m3/sec.
     * @return the droneM3S
     */
    public float getDroneM3S() {
        return droneM3S;
    }

    /**
     * Total ship yield, in m3/hour.
     * @return the totalM3H
     */
    public float getTotalM3H() {
        return totalM3H;
    }

    /**
     * Ship's optimal, in metres.
     * @return the optimal
     */
    public int getOptimal() {
        return optimal;
    }

    /**
     * Ship's ore (or cargo) hold, in m3.
     * @return the cargo
     */
    public int getOreHold() {
        return oreHold;
    }

    /**
     * How long it takes for ore hold to fill, in seconds.
     * @return the secsForCargo
     */
    public int getSecsForOreHold() {
        return secsForOreHold;
    }
}
