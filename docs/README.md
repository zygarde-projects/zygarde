# Zygarde Documentation

This directory contains the source files for the Zygarde documentation site.

## Documentation Structure

```
docs/
├── index.md                # Home page
├── getting-started/        # Getting started guides
├── guide/                  # User guides
├── tutorials/              # Step-by-step tutorials
├── reference/              # Reference documentation
├── development/            # Developer documentation
├── changelog.md            # Version history
├── stylesheets/            # Custom CSS
└── javascripts/            # Custom JavaScript
```

## Local Development

### Prerequisites

- Python 3.7+
- pip (Python package manager)

### Installation

1. Install Python dependencies:

```bash
pip install -r ../requirements.txt
```

Or using a virtual environment (recommended):

```bash
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
pip install -r ../requirements.txt
```

### Running the Documentation Site Locally

#### 1. Serve the documentation:

```bash
mkdocs serve
```

The site will be available at `http://127.0.0.1:8000/`

#### 2. With live reload:

The development server automatically reloads when you save changes to any documentation file.

#### 3. On a different port:

```bash
mkdocs serve --dev-addr=127.0.0.1:8080
```

### Building the Site

Generate static HTML files:

```bash
mkdocs build
```

Output will be in the `site/` directory.

## Writing Documentation

### Markdown Files

Documentation is written in Markdown with support for:

- **GitHub Flavored Markdown** - Tables, task lists, strikethrough
- **Admonitions** - Note, warning, tip boxes
- **Code highlighting** - Syntax highlighting for many languages
- **Mermaid diagrams** - Flowcharts and diagrams

### Code Blocks

````markdown
```kotlin
fun example() {
  println("Hello, Zygarde!")
}
```
````

Supported languages: `kotlin`, `java`, `bash`, `yaml`, `json`, `xml`, `sql`, `gradle`

### Admonitions

```markdown
!!! note "Optional Title"
    This is a note admonition.

!!! warning
    This is a warning.

!!! tip
    This is a helpful tip.

!!! danger
    This is dangerous!
```

### Links

```markdown
# Internal links (relative to docs/)
[Getting Started](getting-started/index.md)
[Quick Start](getting-started/quick-start.md)

# External links
[Kotlin Documentation](https://kotlinlang.org/docs/)

# Link to specific heading
[Installation](getting-started/index.md#installation)
```

### Images

```markdown
![Alt text](images/screenshot.png)
```

Place images in `docs/images/` directory.

## Navigation

Navigation is configured in `mkdocs.yml`:

```yaml
nav:
  - Home: index.md
  - Getting Started:
    - getting-started/index.md
    - Quick Start: getting-started/quick-start.md
```

## Configuration

Site configuration is in `mkdocs.yml` at the project root:

- Site metadata (name, description, URL)
- Theme configuration (colors, features)
- Navigation structure
- Plugins and extensions
- Custom CSS/JS

## Deployment

### GitHub Actions

Documentation is automatically deployed to GitHub Pages when:
- Changes are pushed to the `v2` branch
- Changes are made to `docs/`, `mkdocs.yml`, or related files

Manual deployment trigger is also available via GitHub Actions UI.

### Manual Deployment

```bash
# Build and deploy to GitHub Pages
mkdocs gh-deploy

# Force deployment
mkdocs gh-deploy --force
```

This will:
1. Build the documentation
2. Push to the `gh-pages` branch
3. GitHub Pages will serve the site

## Documentation Guidelines

### Writing Style

- Use clear, concise language
- Write in present tense
- Use active voice
- Include code examples
- Provide context and explanations

### Structure

- Start with overview/introduction
- Use headings hierarchically (H2, H3, H4)
- Keep paragraphs short (3-5 sentences)
- Use lists for steps or options
- Include "Next Steps" or "See Also" sections

### Code Examples

- Keep examples simple and focused
- Include comments for complex code
- Show both correct and incorrect examples when helpful
- Test code examples to ensure they work

### Updates

When updating documentation:

1. Check for broken links
2. Update related pages
3. Test locally with `mkdocs serve`
4. Update changelog if needed
5. Commit with descriptive message

## Troubleshooting

### Port Already in Use

```bash
# Use a different port
mkdocs serve --dev-addr=127.0.0.1:8080
```

### Module Import Errors

```bash
# Reinstall dependencies
pip install --force-reinstall -r ../requirements.txt
```

### Changes Not Appearing

- Hard refresh browser (Ctrl+Shift+R or Cmd+Shift+R)
- Clear browser cache
- Restart MkDocs server

## Resources

- [MkDocs Documentation](https://www.mkdocs.org/)
- [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/)
- [Markdown Guide](https://www.markdownguide.org/)

## Contributing

See [Contributing Guide](development/contributing.md) for:
- How to contribute documentation
- Style guidelines
- Review process

## Questions?

- Open an issue on GitHub
- Check existing documentation
- Ask in GitHub Discussions
