# Eco-Nomz UI/UX Design Plan

## 1. Purpose

Redesign Eco-Nomz into a coherent, trustworthy, mobile-first marketplace. The interface should help customers find and reserve surplus food quickly, help restaurant owners manage offers and pickups confidently, and give administrators clear, safe moderation tools. Replace Thymeleaf page rendering with a React and TypeScript frontend, built and served same-origin by Spring Boot, using JSON APIs backed by the existing Spring application.

This plan describes the design and implementation direction. It does not change application behavior, routes, or data contracts by itself.

## 2. Product and design goals

- Make the marketplace feel appetizing, local, and dependable rather than like a generic admin site.
- Make the next action obvious on every page.
- Show price, savings, quantity, and pickup time where they matter most.
- Explain reservation and payment states in plain language.
- Give each role a consistent navigation and dashboard.
- Work well on narrow mobile screens, including during restaurant pickup.
- Preserve the Spring domain, persistence, Midtrans integration, authentication, authorization, CSRF protections, and customer-facing URLs while replacing MVC page rendering with the React frontend and JSON APIs.
- Use the brand colors `#60ab21` and `#ff7a00` as recognizable accents while maintaining accessible contrast.

## 3. Current experience and design problems

The current UI has a styled marketplace stylesheet, but most related surfaces do not use it. Offer details, sign-in, registration, restaurant pages, owner tools, orders, administration, and errors appear as browser-default HTML. Navigation and visual patterns change between pages or are missing entirely.

The marketplace itself already has useful foundations—search and filters, offer cards, pricing, pickup times, and responsive grids—but its green shades do not match the requested brand, its cards lack food imagery, and the search form gives every filter similar emphasis.

The redesign should address these experience gaps:

- Inconsistent site shell, typography, spacing, controls, and navigation.
- No cohesive customer/owner/admin layout or clear orientation within each area.
- Offers lack compelling imagery and a clear visual hierarchy for savings and pickup urgency.
- Dense forms do not group related fields or clearly distinguish validation, help, and submission states.
- Reservation/payment status is presented as raw system values such as `PENDING_PAYMENT`.
- Empty, error, and confirmation states provide little visual guidance.
- Owner order workflows and pickup verification need to work well on a phone.
- Tables and long lists need a compact responsive presentation.
- Some pages had no other button to go into another pages, for example return button.

## 4. Users and primary journeys

### Customer

1. Discover offers from the home/marketplace page.
2. Search or filter by offer/restaurant, budget, quantity, or pickup time.
3. Compare a card's image, discount, availability, restaurant, and pickup window.
4. Review offer details and reserve a quantity.
5. Pay through Midtrans Sandbox and understand that payment is pending until confirmed.
6. Find the reservation, pickup instructions, and one-time pickup code.

### Restaurant owner

1. Register/sign in and understand the onboarding sequence.
2. Add and publish a restaurant profile.
3. Create and publish an offer with price, stock, and pickup window.
4. Review offer status and incoming reservations.
5. Verify the customer's pickup code while at the counter.

### Administrator

1. Review platform activity from a compact dashboard.
2. Find an account, restaurant, or offer.
3. Suspend/reactivate accounts or hide/restore listings with a clear impact and confirmation.
4. Understand moderated states and any moderation note without modifying business data inadvertently.

## 5. Visual direction

### Brand character

**Fresh, energetic, and practical.** Use warm food photography and editorial typography for discovery; use calm, highly legible surfaces for checkout, forms, and operational work. Keep the interface friendly without sacrificing trust around payments and account actions.

### Color system

Use the requested brand colors as source accents, with neutral and contrast-safe derived colors:

| Token | Value | Intended use |
|---|---|---|
| Brand green | `#60ab21` | Brand marks, active navigation, small accent areas, decorative highlights |
| Brand orange | `#ff7a00` | Savings/urgency accents and limited promotional highlights |
| Primary text | `#17251d` | Headings and body text |
| Muted text | `#526056` | Supporting copy and metadata |
| Page background | `#f7f8f3` | Main canvas |
| Surface | `#ffffff` | Cards, forms, navigation |
| Soft green surface | `#edf5e6` | Low-emphasis brand panels and selected states |
| Soft orange surface | `#fff1e5` | Discount and pickup reminders |
| Border | `#dce4d8` | Dividers, cards, and form controls |
| Success | `#216e39` | Confirmed/complete states |
| Warning | `#8a4b00` | Pending/expiring states |
| Danger | `#a12622` | Errors and destructive actions |

The two specified brand colors are bright and should not be used as backgrounds for small white text or as the only indication of status. Use dark, contrast-safe text on pale brand tints; use a separately tested deep green for primary-button backgrounds if white button text is required. Validate text and control contrast to WCAG 2.2 AA before release.

### Typography and layout

- Use a clear system sans-serif stack for fast loading and broad glyph support.
- Use a restrained type scale: large page title, clear section headings, readable body text, compact metadata.
- Set a comfortable reading width for forms and details; use wider grids only for browse and admin list pages.
- Standardize spacing, corner radii, borders, and shadows as CSS custom properties.
- Prefer short labels, plain language, and predictable alignment over decorative complexity.
- Use icons only when paired with clear labels or accessible names.

## 6. Shared design system

Create reusable React components and shared CSS before styling every individual page:

- **Site shell:** header, brand link/logo, role-aware navigation, main content container, footer.
- **Page heading:** title, short context, breadcrumb/back link, optional primary action.
- **Buttons:** primary, secondary, text, danger, disabled, and loading/processing states.
- **Form controls:** labels, help text, required markers, validation error, checkbox/radio, select, date/time, and currency input.
- **Feedback:** success, warning, error, informational, and inline field messages with semantic markup.
- **Cards:** offer, restaurant, reservation, and dashboard metric cards.
- **Status badges:** readable labels for draft, published, sold out, expired, pending payment, confirmed, completed, suspended, and moderated.
- **Tables/lists:** desktop table and mobile stacked-row treatment for owner/admin pages.
- **Empty states:** concise explanation and one relevant next action.
- **Dialogs/confirmation:** destructive or high-impact admin actions, using native accessible patterns where practical.

Use semantic HTML, labels connected to controls, visible keyboard focus, and Spring Security integration for the JSON APIs. Include CSRF tokens on state-changing requests and preserve authorization boundaries.

## 7. Page-by-page design plan

### Marketplace and offer detail

- Turn the marketplace hero into a compact, welcoming introduction with one prominent search action.
- Prioritize search; move less-used price, quantity, and sort controls into a tidy responsive filter group.
- Show active filters and a one-action clear/reset option.
- Redesign offer cards with a reserved image ratio, restaurant identity, title, discounted price, original price, savings, pickup window, and remaining quantity.
- Make the entire card easy to scan, with one clear detail action and distinct hover/focus states.
- Provide useful no-results guidance and a reset action; do not imply offers exist when the result set is empty.
- On the detail page, use a two-column desktop layout (content and reservation summary) that becomes a single-column mobile flow.
- Keep quantity and reservation controls adjacent to the total; state that a reservation is not paid until checkout succeeds.
- Display restaurant logos and offer photos in square frames, with a neutral branded fallback when an image is unavailable.
- Provide owner upload and management UI: a required square restaurant logo and at least one, up to three, square offer photos to publish. Accept PNG or JPEG up to 10 MB per image.
- For a non-square selection, open an adjustable square cropper with preview before accepting the image; let square images proceed without cropping. Restaurant logos are validated by the backend and stored in Cloudinary; credentials are supplied through server environment variables. Offer-image storage and moderation policy remain explicit implementation decisions.
- Configure the logo provider with `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET`; never send the API secret to the browser. The application stores the Cloudinary secure URL and public ID with the restaurant record.

### Authentication and account dashboard

- Use a centered, narrow form panel with a clear heading, reassuring copy, and inline errors.
- Make customer vs restaurant-owner registration a pair of well-described selectable cards with a strong selected state.
- Keep password requirements adjacent to the password field and preserve entered non-secret values after validation errors.
- Replace the generic account dashboard with role-aware welcome content and a primary next step:
  - Customer: browse offers and view reservations.
  - Owner: complete restaurant setup, create an offer, or view reservations based on current setup.
  - Admin: open management and moderation tools.
- Keep sign-out available but visually secondary.

### Customer reservations and payment status

- Present each reservation as a clear card with offer, quantity, total, pickup window, and a human-readable status.
- Use a status timeline or labeled status panel to distinguish reservation, payment confirmation, ready-to-pick-up, and completed states without claiming unsupported states.
- For pending payment, emphasize the payment action and explain the expiry window and Sandbox status.
- For confirmed orders, make the pickup code prominent, easy to read aloud/show, and visually protected from surrounding details.
- Distinguish cancellation, expired, and manual-review states with actionable explanations.

### Restaurant owner workspace

- Add a consistent owner navigation for Overview, Restaurants, Offers, and Reservations.
- Show the publish workflow as a short checklist: create restaurant → add logo → publish restaurant → create offer → publish offer.
- Use labeled status badges and compact cards/tables for restaurant and offer management.
- Break restaurant and offer forms into logical sections with clear help text, constraints, and review-before-publish affordance.
- Show logo and offer-photo requirements before upload, and provide crop, preview, and image-management states within the owner workflow.
- Use the confirmed crop composition in `.impeccable/mocks/crop-focus-landscape.png` as the north star: a generous crop canvas, slim setup progress, and a quieter square preview with file requirements and next action. The 1200 × 750 sample demonstrates non-square cropping; square uploads bypass this step.
- The crop editor sends the prepared square logo to an owner-authorized Spring endpoint, which validates its image type, dimensions, and size before uploading it to Cloudinary and persisting the secure URL and public ID.
- **Approved crop-surface direction:** the user selected and reconfirmed option 3, “Focused canvas + reassurance panel,” with the corrected landscape sample. The canvas leads; the quieter side panel verifies the square result and requirements. Keep the four-step setup progress, visible square boundary and shaded excluded area, rotate/reset/zoom controls, square preview, PNG/JPEG and 10 MB requirements, required-logo notice, and clear next action. The selected concept and thesis are recorded in `.impeccable/mocks/crop-round-payload.json`; the approved visual reference is `.impeccable/mocks/crop-focus-landscape.png`.
- **First-viewport behavior:** non-square uploads can be repositioned with pointer or keyboard, zoomed and rotated, with the square preview updating live. Square uploads bypass crop controls. Owners can prepare, save, and download the square logo; communicate upload progress, storage configuration failures, and success.
- **Seed provenance:** the approved direction was generated with reference `seed:e06375d984581b91`; `impeccable concept-seed --check-seed seed:e06375d984581b91` confirmed that it was rolled on this machine.
- **Direction contract (approved crop surface):**
  - **THESIS:** Make positioning a landscape restaurant logo inside its required square the primary task; keep the resulting customer-facing square and its requirements available for confirmation.
  - **OWN-WORLD:** Use the established Eco-Nomz brand, soft neutral-green ground, restrained green and orange accents, and a practical owner-workflow tone. This is a focused crop tool, not a marketplace landing page.
  - **STORY:** Orient the owner in restaurant setup, let them adjust the non-square image, show the live square result, then offer the next action. If the source is already square, skip crop adjustment.
  - **FIRST VIEWPORT:** Preserve the approved “Focused canvas + reassurance panel” composition: slim horizontal setup progress; a dominant crop canvas with a visible square boundary and excluded-area shading; rotate, reset, and zoom controls; a quieter square preview with format/size requirements and the required-logo notice; and a clear save action.
  - **FORM:** Responsive desktop-web restaurant-logo setup, built as an owner task in Eco-Nomz. Concept seed reference: `seed:e06375d984581b91`.
  - **Provenance:** This contract summarizes the selected concept in `.impeccable/mocks/crop-round-payload.json` and approved reference `.impeccable/mocks/crop-focus-landscape.png`. The concept seed reference was verified with the Impeccable CLI.
- Pair pickup start/end controls and clarify that times use Jakarta time.
- Make validation errors visible beside their fields and preserve safe form values on failure.
- Design the pickup verification panel for phone use: large input, clear one-time-code instruction, a high-contrast verify action, and a visible result.
- Keep customer names and order details readable without overcrowding operational screens.

### Administration

- Use a distinct but related admin shell, with dashboard metrics and navigation to accounts, restaurants, and offers.
- Improve management lists with search/filter controls, pagination, status badges, and clearly labeled actions.
- Make account suspension or listing moderation require a contextual confirmation that explains its effect.
- Keep restore/reactivate actions distinct from destructive actions; show moderation notes near the affected listing.
- On mobile, replace horizontally scrolling tables with stacked records and actions.

### Restaurants, errors, and system feedback

- Give public restaurant pages the same header, content width, and card language as the marketplace.
- Provide a polished not-found and general-error experience with a safe recovery link and no technical details.
- Use consistent success/error feedback after form submissions and actions.
- Make loading, disabled, and repeated-submit states clear wherever a request could take time.

## 8. Responsive behavior

Design and verify at these representative viewport widths:

- **Small mobile:** 320–375 px; single column, comfortable touch targets, no horizontal overflow.
- **Large mobile:** 390–480 px; preserve card hierarchy and avoid dense multi-column forms.
- **Tablet:** 768 px; two-column marketplace cards where content remains readable.
- **Desktop:** 1024–1440 px; restrained content width, balanced card grids, persistent role navigation.

Use mobile-first CSS. At narrow widths, stack filter fields, forms, dashboard stats, and table records. Keep primary actions visible without requiring awkward horizontal scrolling.

## 9. Accessibility and localization

- Target WCAG 2.2 AA for text contrast, controls, focus visibility, and keyboard interaction.
- Use logical heading order, semantic landmarks, descriptive link text, and accessible names for icon-only controls.
- Never communicate status using color alone; combine color with text and, where useful, an icon.
- Provide visible labels, clear field-specific errors, and programmatic error associations.
- Ensure focus is not hidden by sticky headers or mobile navigation.
- Use Indonesian rupiah formatting consistently and localize customer-facing wording for the pilot audience.
- Keep pickup times explicit, unambiguous, and consistently localized; forms must communicate the Jakarta time zone.
- Respect reduced-motion preferences and avoid motion as the sole source of feedback.

## 10. Implementation sequence

### Step 1 — Foundation

- Add design tokens and a shared base stylesheet.
- Set up the React and TypeScript application, design tokens, reusable shell/navigation/feedback components, and shared CSS.
- Add same-origin JSON APIs and SPA route handling while preserving customer-facing URLs and existing Spring Security, authorization, and CSRF behavior.
- Establish responsive containers, typography, focus styles, and button/form primitives.
- Add the shared shell to each surface without changing customer-facing route behavior.

### Step 2 — Customer discovery and conversion

- Redesign marketplace, restaurant listing/detail, offer detail, registration, and login.
- Verify filter query parameters, validation, reservation forms, and anonymous/customer states still work.

### Step 3 — Orders and pickup

- Redesign customer reservations, owner reservations, and pickup verification states.
- Verify payment pending/confirmed and pickup complete flows remain honest and understandable.

### Step 4 — Owner and admin workspaces

- Add consistent role navigation and responsive list/form layouts.
- Redesign admin management and confirmation interactions without changing permission rules.

### Step 5 — Quality and polish

- Check representative screen sizes and browser behavior.
- Run accessibility and contrast checks, keyboard-only walkthroughs, frontend build/type checks, API integration checks, and existing automated tests.
- Capture before/after screenshots for primary customer, owner, and admin journeys and resolve visual regressions.

## 11. Acceptance criteria

- Every existing customer-facing page is rendered by the React frontend with the shared design system and role-appropriate navigation.
- Customer, owner, and admin primary tasks are identifiable without relying on raw enum values or unexplained controls.
- The marketplace shows offer price, savings, availability, restaurant, and pickup information in a consistent hierarchy.
- No page has horizontal overflow at 320 px viewport width.
- All key workflows are usable with keyboard only and have visible focus.
- Text and interactive controls meet WCAG 2.2 AA contrast requirements; the two bright brand colors are used as accents or on tested contrast-safe surfaces.
- Empty, invalid, success, error, pending, and completed states are styled and understandable.
- Existing customer-facing URLs, query behavior, security rules, CSRF protections, and validation/business behavior remain intact through the frontend/API transition.
- Existing automated tests pass, and customer/owner/admin walkthroughs are completed on desktop and mobile breakpoints.
- Image upload UI enforces the confirmed square output, PNG/JPEG type, 10 MB per-image limit, restaurant-logo requirement, and one-to-three offer-photo range. Restaurant-logo persistence uses Cloudinary; offer-image storage and moderation policy remain implementation decisions.

## 12. Out of scope for the first redesign

- Changing business rules, routes, authentication, payment semantics, or database entities.
- Replacing Spring domain services, persistence, payment integration, or security with a new backend.
- Adding map/geolocation, recommendations, reviews, or chat without separate product decisions. Offer-image storage and moderation implementation still require explicit technical decisions.
- Reproducing another food marketplace's branding or exact screens.
