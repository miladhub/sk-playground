# sk-playground

Semantic kernel playground.

```bash
mvn clean install
export OPENAI_API_KEY=...
java -jar target/*.jar
```

Sample interaction:

```text
User > hello
Assistant > Hello! How can I assist you today?
User > how are the lights?
Getting lights
Assistant > The current state of the lights are as follows:
1. Table Lamp is off
2. Porch light is off
3. Chandelier is on

Is there anything specific you would like me to do with the lights?
User > turn them off
Getting lights
Changing light 1 false
Changing light 2 false
Changing light 3 false
Assistant > The lights have been successfully turned off. All lights including the Table Lamp, Porch Light, and Chandelier are now off.

Is there anything else you would like to do with the lights?
User > now turn them on
Getting lights
Changing light 1 true
Changing light 2 true
Changing light 3 true
Assistant > All lights have been successfully turned on. The Table Lamp, Porch Light, and Chandelier are now illuminated.
```
