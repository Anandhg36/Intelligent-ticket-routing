from transformers import AutoTokenizer, AutoModelForCausalLM, pipeline
import torch


class LLMClient:
    def __init__(self):
        model_name = "microsoft/phi-2"

        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForCausalLM.from_pretrained(
            model_name,
            device_map="auto",
            torch_dtype=torch.float16 if torch.cuda.is_available() else torch.float32
        )

        self.generator = pipeline(
            "text-generation",
            model=self.model,
            tokenizer=self.tokenizer
        )

    def generate(self, prompt: str) -> str:
        output = self.generator(
                prompt,
                max_new_tokens=80,
                do_sample=False,
                temperature=0.3
            )


        # Remove prompt echo
        generated = output[0]["generated_text"]
        return generated.split(prompt)[-1].strip()
