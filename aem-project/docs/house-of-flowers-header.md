# House of Flowers header

Fully authorable sticky mega-menu header. HTL and the Sling model render only dialog values. There are no fallback navigation items, links, or teaser copy in code.

## Component

| | |
| --- | --- |
| Resource type | `houseofflowers/components/header` |
| Alias | `houseofflower/components/header` (hidden, for older XF instances) |
| Group | House of Flower - Content |
| Sling model | `com.adobe.aem.guides.demo.core.models.HeaderModel` |
| Clientlib | `houseofflowers.header` |
| Dialog tabs | Logo, Navigation Items, Mega Menu Settings |

Mega-menu fields live on each navigation item (enable mega-menu, then author that item’s category statement, sections, and featured cards). Authors can give Shop, About, Occasions, or any item its own panel.

An empty component shows an editor placeholder. `_cq_template` seeds florist sample values when the component is first dropped; those values are JCR content and can be changed or deleted in the dialog.

## Install

From the project root, local author `http://localhost:4502` (`admin` / `admin`):

```
mvn -PautoInstallBundle -pl core install -DskipTests
mvn -pl ui.apps package
mvn -pl ui.apps com.day.jcr.vault:content-package-maven-plugin:1.0.6:install
mvn -PautoInstallPackage -pl ui.content install -DskipTests
```

Rebuild `ui.apps` before the FileVault install goal. `-PautoInstallPackage -pl ui.apps` only installs to `.m2`.

The two florist header Experience Fragments use vault `replace` so nested mega-menu nodes update on reinstall:

- `/content/experience-fragments/demo/us/en/site/hof-header`
- `/content/experience-fragments/houseofflower/us/en/site/header`

## Author an Experience Fragment

1. Create a Web variation, or open `/content/experience-fragments/demo/us/en/site/hof-header/master`.
2. Drop **House of Flowers Header**.
3. **Logo** — image (optional), alt, wordmark text, link.
4. **Navigation Items** — add primary items. For each item set title and link. Tick **Enable mega-menu** to author that item’s panel:
   - Category headline, description, and primary CTA (left rail)
   - Content sections (title + nested links)
   - Featured heading and image cards (image, title, link, type/label, alt)
5. **Mega Menu Settings** — reminder that each navigation item owns its own panel. There is no separate global mega-menu.
6. Publish the XF.

Leave `/content/experience-fragments/demo/us/en/site/header` unchanged; that is the existing demo site header.

## Wire an Editable Template

The **House of Flowers Page** template is already wired:

- Template: `/conf/demo/settings/wcm/templates/house-of-flowers`
- Structure XF: `/content/experience-fragments/demo/us/en/site/hof-header/master`
- Include: Core XF v2 (`core/wcm/components/experiencefragment/v2/experiencefragment`)
- Policy `policy_hof_header` leaves the wrapper as a `div` so this component can render the `<header>` landmark

To add the header to another template:

1. Open the template **Structure** view.
2. Add a Core v2 Experience Fragment (not `demo/components/experiencefragment`, which is an XF page).
3. Point **Fragment Variation** at the header XF master.
4. Allow `group:House of Flower - Content` on the XF container.

Core XF v2 localizes by site name. Keep the unique `hof-header` name on `/content/demo` so localization does not pick up the generic demo header.

## Sample pages

- Home: `http://localhost:4502/content/demo/us/en/house-of-flowers.html`
- Editor: `http://localhost:4502/editor.html/content/demo/us/en/house-of-flowers.html`
- Static preview: `docs/house-of-flowers-header-preview.html`
- Authoring snapshot: `docs/house-of-flowers-header.sample.json`

Desktop: hover or focus a mega-menu item to open its panel. Mobile: hamburger, then accordion. Escape and click-outside close the panel.
