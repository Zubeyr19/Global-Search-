# How to Use the Draw.io ER Diagram

## ✅ FIXED - New Working File!

**ERDiagram_DrawIO.xml** - Proper XML format that Draw.io can import!

## 📖 How to Import (3 Simple Steps)

### Step 1: Open Draw.io
Go to: **https://app.diagrams.net/**

### Step 2: Import the File
1. Click **"File"** → **"Open from"** → **"Device"**
2. Navigate to: `C:\Users\Zubeyr\IdeaProjects\Global-Search-\uml\`
3. Select: **ERDiagram_DrawIO.xml**
4. Click **"Open"**

### Step 3: Done!
The complete Chen notation ER diagram will load with:
- ✅ 10 Entities (rectangles)
- ✅ 8 Relationships (diamonds)
- ✅ All Attributes (ovals)
- ✅ Underlined primary keys
- ✅ Cardinality labels (1, N)

## 📤 Export for Your Thesis

Once imported:

1. **Click** "File" → "Export as" → Choose format:
   - **PNG** - For Word documents (300 DPI recommended)
   - **SVG** - For LaTeX (best quality, scalable)
   - **PDF** - For printing

2. **Settings**:
   - Resolution: 300 DPI
   - Border: 10px
   - Transparent background: Yes (or No if you want white)

3. **Save** to your thesis folder

## ✏️ Edit the Diagram

Everything is editable:
- **Drag** elements to rearrange
- **Double-click** text to edit
- **Add** more ovals for attributes
- **Change** colors/styles
- **Resize** shapes

## 💾 Save Your Changes

- **File** → **Save as** → Save to your computer
- Keep the .drawio or .xml format for future editing
- Export to PNG/SVG when ready for thesis

## 🎨 What's Included

**Main Hierarchy:**
```
COMPANY
  ↓ has (1:N)
LOCATION
  ↓ contains (1:N)
ZONE
  ↓ includes (1:N)
SENSOR
  ↓ generates (1:N)
SENSOR_DATA
```

**User Branch:**
```
COMPANY
  ↓ employs (1:N)
USER
  ↓ owns (1:N)
DASHBOARD
  ↓ creates (1:N)
REPORT
  ↓ logs (1:N)
AUDIT_LOG
```

**Plus:**
- POLICY entity (standalone)
- Legend explaining notation
- All key attributes for each entity

Perfect Chen notation matching your example! 🎯
