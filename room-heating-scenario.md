# Room heating scenario

This document provides the technical details of our room-heating scenario in a typical Swiss home.

**Declaration on the use of AI**: The numbers and technical details were generated with Claude Opus 4.5 and refined with GPT-5 (LLM-as-judge). The authors checked the numbers for plausibility and consistency, but did not consult a domain expert. This scenario is only meant for illustrative purposes.

## Physical setup

- Single-family Swiss home
- Central radiator system with night setback to 16°C
- Supplementary electric heaters (1.0 kW each) in 4 rooms
- Shared household circuit: 16A @ 230V = 3.68 kW max
- Building: Minergie standard (U-wall: 0.20 W/m²K, triple-glazed windows)

## Context

Outdoor temperature: -2°C
Circuit load: 0/3.68 kW available

Living Room (25m², 60m³): 
  Current: 15°C (dropped overnight due to TILTED window)
  Target: 20°C
  Window: TILTED (forgotten overnight since 22:00)
  Heater: OFF
  
Bedroom 1 (15m², 36m³):
  Current: 16.5°C (normal overnight cooling)
  Target: 20°C  
  Window: CLOSED
  Heater: OFF

Bedroom 2 (15m², 36m³):
  Current: 17°C (normal overnight cooling)
  Target: 20°C
  Window: CLOSED
  Heater: OFF

Home Office (12m², 29m³):
  Current: 18°C (computer equipment adds heat)
  Target: 21°C (kept warmer for work)
  Window: CLOSED
  Heater: OFF

## Timeline

T1 (07:05): All room thermostats request supplementary heat
  (Radiators warming but slow, residents want faster heat)
  
  Living Room: requests 1.0 kW → GRANTED (load: 1.0/3.68)
  Bedroom 1: requests 1.0 kW → GRANTED (load: 2.0/3.68)
  Bedroom 2: requests 1.0 kW → GRANTED (load: 3.0/3.68)
  Home Office: requests 1.0 kW → DENIED (would exceed limit)
  
  Home Office enters queue, waiting for capacity


T2 (07:05 - 07:25): First heating period (20 minutes)

  Living Room (TILTED window):
    Heat input: 1.0 kW × 0.333hr = 0.333 kWh
    
    Heat loss rate (at average temp ~16°C):
      - Walls: 0.20 W/m²K × 50m² × 18°C = 180W
      - Window (conduction): 0.7 W/m²K × 2m² × 18°C = 25W
      - Air (tilted adds 0.4 ACH): 
        60m³ × 0.4/hr × 1.2 kg/m³ × 1.005 kJ/kgK × 18°C / 3.6 = 144W
      - Total: 349W = 0.349 kW
      
    Heat loss: 0.349 kW × 0.333hr = 0.116 kWh
    Net heat gain: 0.333 - 0.116 = 0.217 kWh
    
    Temperature rise: 0.217 kWh × 3600 kJ/kWh / (60m³ × 1.2 × 1.005 kJ/kgK)
                    = 781 kJ / 72.4 kJ/K = 1.1°C
    Final temp: 15 + 1.1 = 16.1°C (far from target!)
    
  Bedroom 1 (CLOSED window):
    Heat input: 0.333 kWh
    
    Heat loss rate (at average ~18°C):
      - Walls: 0.20 × 30m² × 20°C = 120W
      - Window: 0.7 × 1.5m² × 20°C = 21W
      - Air (0.1 ACH): 36m³ × 0.1/hr × 1.2 × 1.005 × 20°C / 3.6 = 24W
      - Total: 165W = 0.165 kW
      
    Heat loss: 0.165 × 0.333 = 0.055 kWh
    Net: 0.333 - 0.055 = 0.278 kWh
    Temp rise: 0.278 × 3600 / 43.5 = 2.3°C
    Final: 16.5 + 2.3 = 18.8°C
    
  Bedroom 2 (CLOSED window):
    Similar calculation
    Final: 17 + 2.4 = 19.4°C


T3 (07:25): Bedroom 2 nears target, continues for another 5 min

T4 (07:30): Bedroom 2 reaches 20°C target
  Bedroom 2 heater: OFF
  Circuit load: 2.0/3.68 kW
  Energy consumed (Bedroom 2): 1.0 kW × 0.417hr = 0.417 kWh
  
  Home Office heater: NOW GRANTED (load: 3.0/3.68)
  Home Office starts heating (delayed 25 minutes!)


T5 (07:35): Bedroom 1 reaches 20°C target
  Bedroom 1 heater: OFF
  Circuit load: 2.0/3.68 kW
  Energy consumed (Bedroom 1): 1.0 kW × 0.5hr = 0.50 kWh
  
  Living Room + Home Office continue


T6 (07:05 - 08:30): Living Room continuous operation (85 minutes!)
  
  Living Room progression:
    07:05 → 15.0°C (start)
    07:25 → 16.1°C
    07:45 → 17.1°C
    08:05 → 18.2°C
    08:25 → 19.3°C
    08:30 → 19.5°C (gives up? Or continues...)
    
  Total energy consumed: 1.0 kW × 1.42hr = 1.42 kWh
  
  Expected energy (if window closed):
    Would reach 20°C in ~30 minutes
    Energy needed: ~0.55 kWh
    
  Wasted energy: 1.42 - 0.55 = 0.87 kWh (!!!)
  Percentage waste: 0.87 / 1.42 = 61% of energy wasted


T7 (08:05): Home Office reaches 21°C target
  Home Office heater: OFF
  Energy consumed: 1.0 kW × 0.583hr = 0.583 kWh
  (Delayed start cost: should have finished at 07:45)


T8 (08:30): Summary after ~85 minutes

  Bedroom 2: ✓ Comfortable since 07:30 (25 min heating)
    Energy: 0.42 kWh, Efficiency: 96%
    
  Bedroom 1: ✓ Comfortable since 07:35 (30 min heating)  
    Energy: 0.50 kWh, Efficiency: 96%
    
  Home Office: ✓ Comfortable since 08:05 (35 min heating)
    Energy: 0.58 kWh, Efficiency: 95%
    BUT: Delayed 25 minutes, occupant cold during work start
    
  Living Room: ✗ Only 19.5°C, still running!
    Energy: 1.42 kWh (so far), Efficiency: 39%
    Wasted: 0.87 kWh through tilted window
    
  TOTAL SYSTEM:
    Energy consumed: 2.92 kWh
    Energy wasted: 0.87 kWh (30% of total!)
    Delayed comfort: Home Office -25 minutes
    Failed comfort: Living Room never reaches target