package org.meh;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.KeyCredential;
import com.google.gson.Gson;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.contextvariables.ContextVariableTypeConverter;
import com.microsoft.semantickernel.contextvariables.ContextVariableTypes;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.InvocationReturnMode;
import com.microsoft.semantickernel.orchestration.ToolCallBehavior;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;

import java.util.*;

public class LightsExample
{
    private static final String OPENAI_KEY = System.getenv("OPENAI_API_KEY");
    private static final String MODEL_ID = "gpt-3.5-turbo-0125";

    public static void main(String[] args) {
        OpenAIAsyncClient client = new OpenAIClientBuilder()
                .credential(new KeyCredential(OPENAI_KEY))
                .buildAsyncClient();

        // Import the LightsPlugin
        KernelPlugin lightPlugin =
                KernelPluginFactory.createFromObject(new LightsPlugin(),
                "LightsPlugin");

        // Create your AI service client
        ChatCompletionService chatCompletionService =
                OpenAIChatCompletion.builder()
                .withModelId(MODEL_ID)
                .withOpenAIAsyncClient(client)
                .build();

        // Create a kernel with Azure OpenAI chat completion and plugin
        Kernel kernel = Kernel.builder()
                .withAIService(ChatCompletionService.class,
                        chatCompletionService)
                .withPlugin(lightPlugin)
                .build();

        // Add a converter to the kernel to show it how to serialise LightModel
        // objects into a prompt
        ContextVariableTypes
                .addGlobalConverter(
                        ContextVariableTypeConverter.builder(LightModel.class)
                                .toPromptString(new Gson()::toJson)
                                .build());

        // Enable planning
        InvocationContext invocationContext = new InvocationContext.Builder()
                .withReturnMode(InvocationReturnMode.LAST_MESSAGE_ONLY)
                .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
                .build();

        // Create a history to store the conversation
        ChatHistory history = new ChatHistory();

        // Initiate a back-and-forth chat
        Scanner scanner = new Scanner(System.in);
        String userInput;
        do {
            // Collect user input
            System.out.print("User > ");

            userInput = scanner.nextLine();
            // Add user input
            history.addUserMessage(userInput);

            // Prompt AI for response to users input
            List<ChatMessageContent<?>> results = chatCompletionService
                    .getChatMessageContentsAsync(history, kernel,
                            invocationContext)
                    .block();

            for (ChatMessageContent<?> result : results) {
                // Print the results
                if (result.getAuthorRole() == AuthorRole.ASSISTANT && result.getContent() != null) {
                    System.out.println("Assistant > " + result);
                }
                // Add the message from the agent to the chat history
                history.addMessage(result);
            }
        } while (userInput != null && !userInput.isEmpty());
    }

    public static class LightsPlugin
    {
        // Mock data for the lights
        private final Map<Integer, LightsExample.LightModel> lights = new HashMap<>();

        public LightsPlugin() {
            lights.put(1, new LightsExample.LightModel(1, "Table Lamp", false));
            lights.put(2, new LightsExample.LightModel(2, "Porch light", false));
            lights.put(3, new LightsExample.LightModel(3, "Chandelier", true));
        }

        @DefineKernelFunction(name = "get_lights", description = "Gets a list" +
                " of lights and their current state")
        public List<LightsExample.LightModel> getLights() {
            System.out.println("Getting lights");
            return new ArrayList<>(lights.values());
        }

        @DefineKernelFunction(name = "change_state", description = "Changes " +
                "the state of the light")
        public LightsExample.LightModel changeState(
                @KernelFunctionParameter(name = "id", description = "The ID " +
                        "of the light to change") int id,
                @KernelFunctionParameter(name = "isOn", description = "The " +
                        "new state of the light") boolean isOn
        ) {
            System.out.println("Changing light " + id + " " + isOn);
            if (!lights.containsKey(id)) {
                throw new IllegalArgumentException("Light not found");
            }

            lights.get(id).setOn(isOn);

            return lights.get(id);
        }
    }

    public static final class LightModel
    {
        private int lightId;
        private String lightName;
        private boolean isOn;

        public LightModel(
                int lightId,
                String lightName,
                boolean isOn
        ) {
            this.lightId = lightId;
            this.lightName = lightName;
            this.isOn = isOn;
        }

        public void setOn(boolean on) {
            isOn = on;
        }
    }
}
