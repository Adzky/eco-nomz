# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- Customers looking for discounted surplus food from local restaurants.
- Restaurant owners managing restaurants, offers, and customer pickups.
- Administrators overseeing platform activity and moderating accounts and listings.

## Product Purpose

Eco-Nomz helps restaurants make surplus food available to customers at a discount, reducing unsold food. Success means customers can find and reserve suitable offers, complete checkout, and collect their order; restaurant owners can manage offers and fulfill pickups; and administrators can moderate the platform safely.

## Positioning

The established product mechanism is a local surplus-food marketplace with offer discovery, reservation and checkout, and code-based pickup. No additional differentiator has been established.

## Operating Context

- Customers browse and filter local offers, review details, reserve food, complete payment, and present a pickup code.
- Restaurant owners manage restaurant listings and food offers and verify pickups.
- Administrators review platform activity and moderate accounts and listings.
- Prices use Indonesian rupiah. Pickup times are expressed in the Jakarta time zone.

## Capabilities and Constraints

- The existing application uses Spring MVC, Thymeleaf, and Spring Security. The confirmed direction is a React and TypeScript frontend built and served same-origin by Spring Boot, with JSON APIs backed by the existing Spring application.
- Keep Spring responsible for domain logic, persistence, Midtrans, authentication, authorization, and CSRF. Preserve existing customer-facing URLs, business behavior, and security boundaries while replacing MVC page rendering with the interactive frontend and API endpoints.
- Checkout uses Midtrans Sandbox; payment and reservation states must not be represented as confirmed until the existing application confirms them.
- Restaurant onboarding requires a square logo to publish. Offers require at least one square photo and may have up to three. Each image must be PNG or JPEG and no larger than 10 MB.
- When a selected image is not square, provide an interactive square crop step with preview and adjustment before accepting it. Square images can proceed without cropping.
- Restaurant logos are uploaded through the authenticated Spring backend to Cloudinary; the Cloudinary API secret stays server-side. Offer-image storage and moderation policy remain undecided.
- Configure logo storage with the server-side `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET` environment variables.

## Brand Commitments

- Product name: Eco-Nomz.
- Brand colors: `#60ab21` and `#ff7a00`.

## Evidence on Hand

- The existing application and [DESIGNPLAN.md](./DESIGNPLAN.md) document the product workflows and technical constraints.
- No confirmed food photography, testimonials, customer claims, or other marketing proof assets are established. Do not invent them.

## Product Principles

- Make discounted local surplus food straightforward to discover and reserve.
- Keep payment, reservation, and pickup status accurate and understandable.
- Help restaurant owners complete offer and pickup tasks reliably.
- Make moderation actions safe and clear.
- Preserve the existing application’s business rules and security boundaries.

## Accessibility & Inclusion

- The existing design plan sets WCAG 2.2 AA as the interface accessibility target.
