import sys
from PIL import Image, ImageDraw, ImageFont

def render_text_to_image(text, output_file):
    # Setup fonts and dimensions
    try:
        font = ImageFont.truetype("consola.ttf", 16)
    except IOError:
        font = ImageFont.load_default()
    
    # Calculate size
    lines = text.split('\n')
    line_height = 20
    img_height = len(lines) * line_height + 40
    img_width = 800
    
    # Create image
    img = Image.new('RGB', (img_width, img_height), color=(30, 30, 30))
    d = ImageDraw.Draw(img)
    
    # Draw text
    y = 20
    for line in lines:
        d.text((20, y), line, fill=(200, 200, 200), font=font)
        y += line_height
        
    img.save(output_file)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python render_error.py <input_text_file> <output_image_file>")
        sys.exit(1)
    
    with open(sys.argv[1], 'r') as f:
        text = f.read()
    
    render_text_to_image(text, sys.argv[2])
