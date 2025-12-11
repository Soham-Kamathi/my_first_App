"""
Script to download and convert BGE-Small model to ONNX format
"""
import os
from pathlib import Path
from transformers import AutoTokenizer, AutoModel
import torch
import onnx
from onnxruntime.quantization import quantize_dynamic, QuantType

def download_and_convert():
    model_name = "BAAI/bge-small-en-v1.5"
    output_dir = Path("bge-small-onnx")
    output_dir.mkdir(exist_ok=True)
    
    print(f"Downloading {model_name} from Hugging Face...")
    tokenizer = AutoTokenizer.from_pretrained(model_name)
    model = AutoModel.from_pretrained(model_name)
    
    print("Saving model and tokenizer locally...")
    tokenizer.save_pretrained(output_dir)
    model.save_pretrained(output_dir)
    
    print("Converting to ONNX format...")
    model.eval()
    
    # Create dummy input
    dummy_text = "This is a sample sentence for conversion."
    inputs = tokenizer(dummy_text, return_tensors="pt", padding=True, truncation=True, max_length=512)
    
    # Export to ONNX
    onnx_path = output_dir / "model.onnx"
    torch.onnx.export(
        model,
        (inputs['input_ids'], inputs['attention_mask']),
        str(onnx_path),
        input_names=['input_ids', 'attention_mask'],
        output_names=['last_hidden_state'],
        dynamic_axes={
            'input_ids': {0: 'batch_size', 1: 'sequence_length'},
            'attention_mask': {0: 'batch_size', 1: 'sequence_length'},
            'last_hidden_state': {0: 'batch_size', 1: 'sequence_length'}
        },
        opset_version=14,
        do_constant_folding=True,
    )
    
    print(f"Model exported to {onnx_path}")
    
    # Quantize the model for mobile efficiency
    print("Quantizing model for mobile deployment...")
    quantized_path = output_dir / "bge-small-en-v1.5-quantized.onnx"
    quantize_dynamic(
        str(onnx_path),
        str(quantized_path),
        weight_type=QuantType.QUInt8
    )
    
    print(f"Quantized model saved to {quantized_path}")
    print(f"\nModel files:")
    print(f"  - Full precision: {onnx_path}")
    print(f"  - Quantized: {quantized_path}")
    print(f"\nRecommended for Android: {quantized_path.name}")
    print(f"\nNext step: Copy {quantized_path.name} to app/src/main/assets/bge-small-en-v1.5.onnx")
    
    return quantized_path

if __name__ == "__main__":
    try:
        model_path = download_and_convert()
        print("\n✅ Conversion completed successfully!")
    except Exception as e:
        print(f"\n❌ Error during conversion: {e}")
        import traceback
        traceback.print_exc()
