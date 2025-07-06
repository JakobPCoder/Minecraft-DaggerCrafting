# A Survey of Temporal Antialiasing Techniques — Detailed Summary & Review

**Source:** L. Yang, S. Liu, M. Salvi, Computer Graphics Forum, Vol. 39, No. 2, 2020  
[Official Article Link](http://onlinelibrary.wiley.com)

---

## Introduction

**Temporal Antialiasing (TAA)** is a family of techniques that perform spatial antialiasing by accumulating data across multiple frames, amortizing supersampling over time. TAA is now the standard antialiasing solution in most real-time renderers and game engines, replacing older techniques like MSAA, especially in deferred rendering pipelines.

---

## 1. TAA: Core Concepts

- **Goal:** Achieve high-quality antialiasing by reusing shading results from previous frames, thus reducing the cost of brute-force supersampling.
- **Key Benefits:**
  - Superior aliasing suppression and temporal stability compared to single-frame post-processing AA.
  - Easy integration as a post-processing pass, requiring only the previous frame's output as feedback.
- **Main Challenges:**
  - Can produce soft/blurry images.
  - Susceptible to artifacts like ghosting, shimmering, and temporal lag.

---

## 2. Algorithmic Overview

### 2.1. Pipeline Components

1. **Jittered Sampling**
   - Each frame, the camera projection is jittered by a subpixel offset (from a low-discrepancy sequence like Halton or Sobol) to ensure even coverage over time.
   - This produces a different sample location for each pixel every frame.

2. **Reprojection (History Fetch)**
   - For each pixel in the current frame, use motion vectors to map its center to the previous frame and fetch the corresponding color from the history buffer.
   - Motion vectors are computed per-pixel, often using depth and camera/object transforms.

3. **History Validation**
   - Before using the fetched history, validate it to avoid introducing artifacts.
   - Validation uses geometry (depth, normals, object ID) and/or color data to detect occlusion, disocclusion, or shading changes.

4. **Blending (Sample Accumulation)**
   - The new sample is blended with the validated history color using a blending factor (α):
     - `f_n(p) = α * s_n(p) + (1-α) * f_{n-1}(π(p))`
   - α controls the tradeoff between stability (low α) and responsiveness (high α).
   - Exponential smoothing is common, but optimal variance reduction requires equal weighting (can be achieved by tracking per-pixel sample count).

5. **Rectification (Optional)**
   - If history is not fully valid but not completely invalid, it can be rectified (clamped or clipped) to the color bounds of the current frame's neighborhood.

6. **Output**
   - The blended result is written to the history buffer for use in the next frame.

---

## 3. Detailed Algorithmic Steps

### 3.1. Jittering
- **Purpose:** Ensures that over multiple frames, each pixel receives samples at different subpixel locations, effectively supersampling the image.
- **Implementation:**
  - Jitter offsets are drawn from well-distributed sequences (Halton, Sobol, etc.).
  - The sequence should be short and evenly distributed for fast convergence.
  - Jitter is applied to the camera's projection matrix each frame.

### 3.2. Reprojection
- **Purpose:** Fetch the history color for each pixel by mapping its current location to the previous frame.
- **Implementation:**
  - Use per-pixel motion vectors (velocity buffer) to determine where the pixel was in the previous frame.
  - If motion vectors are not available for all pixels, reconstruct using depth and camera matrices.
  - Reprojection may result in subpixel coordinates; use bilinear or higher-order filtering to fetch the color.

### 3.3. History Validation and Disocclusion Handling
- **Disocclusion:**
  - Occurs when a previously hidden surface becomes visible (e.g., due to camera/object movement).
  - The reprojected history color is invalid (from a different surface/object).
- **Detection:**
  - Compare the depth (and optionally normal/object ID) of the current pixel and the reprojected pixel.
  - If the difference exceeds a threshold, mark as disoccluded.
- **Handling:**
  - **Reject history:** Set α = 1, use only the current frame's color.
  - **Soft refresh:** Increase α to favor the new sample but still blend with history.
  - **Rectification:** Clamp or clip the history color to the color bounds of the current frame's neighborhood (see below).
- **Color-based Validation:**
  - Compare the color difference between the current sample and history.
  - If the difference is large, history is rejected or blended with a higher weight for the new sample.

### 3.4. Neighborhood Sampling and Rectification
- **Purpose:** Prevents invalid or stale history from introducing artifacts, especially near edges or in disoccluded regions.
- **Neighborhood Sampling:**
  - For each pixel, gather a 3x3 (or larger) neighborhood of current frame samples.
  - Compute the color extent (bounding box or convex hull) in color space (RGB or YCoCg).
- **Rectification Methods:**
  - **Clamping:** Clamp the history color to the min/max of the neighborhood.
  - **Clipping:** If the history color is outside the convex hull, project it onto the hull.
  - **Variance Clipping:** Use mean and standard deviation to define the color extent, reducing the influence of outliers.
- **Tradeoffs:**
  - Clamping/clipping can introduce blurriness, especially for thin or undersampled features.
  - Too aggressive rectification can cause loss of detail; too lenient can allow ghosting.

### 3.5. Blending and Accumulation
- **Blending Factor (α):**
  - Fixed α is common (e.g., 0.1), but adaptive α can improve quality (e.g., increase α with motion speed or after history rejection).
  - Tracking per-pixel sample count allows for optimal variance reduction (α = 1/N).
- **HDR Considerations:**
  - TAA is ideally applied in linear HDR space, but may require tonemapping before/after to avoid artifacts.
  - Luminance-adaptive weights can be used to avoid color desaturation and "fireflies."

---

## 4. Temporal Upsampling and Checkerboard Rendering

### 4.1. Temporal Upsampling
- **Goal:** Accumulate low-resolution samples into a higher-resolution output, reducing shading cost for high-res displays.
- **Algorithm:**
  - Upscale input samples to output resolution using a reconstruction filter (Gaussian, bilinear, etc.).
  - For each output pixel, compute a weighted sum of nearby input samples.
  - Use a confidence factor β(p) to control blending:
    - `f_n(p) = α * β(p) * s̄_n(p) + (1 - α * β(p)) * f_{n-1}(π(p))`
  - β(p) is high when the sample is close to the output pixel center, low otherwise.
- **Challenges:**
  - Sparse input makes history validation and rectification harder.
  - Neighborhoods for color bounds must be carefully chosen to avoid over-blurring.

### 4.2. Checkerboard Rendering
- **Technique:** Shades alternating pixels in a checkerboard pattern each frame, using TAA to reconstruct the full image.
- **Implementation:**
  - Each frame, shade half the pixels; alternate pattern next frame.
  - Use TAA's reprojection, accumulation, and rectification to fill in missing pixels.
- **Benefits:**
  - Efficient for 4K rendering on consoles.
  - Inherently more friendly to interpolation and clamping than earlier interlaced approaches.

---

## 5. Common Artifacts and Solutions

### 5.1. Blurriness
- **Sources:**
  - Repeated resampling (especially with bilinear filters) during reprojection.
  - Aggressive history rectification (clamping/clipping).
- **Mitigations:**
  - Use higher-order filters (Catmull-Rom, BFECC, Sacht-Nehab) for resampling.
  - Adaptive α to limit blur accumulation.
  - Sharpening filters (e.g., Laplacian) post-TAA.

### 5.2. Ghosting
- **Cause:** Invalid history not fully rejected, especially in disoccluded regions or with incorrect motion vectors.
- **Mitigations:**
  - Responsive AA flags for materials prone to ghosting (e.g., translucency).
  - Dilated stencil masks to ensure all edge pixels are refreshed.
  - Combine history rejection and rectification.

### 5.3. Temporal Instability
- **Cause:** Overzealous history rejection/rectification, especially with undersampled or aliased input.
- **Mitigations:**
  - Motion coherency tests to bias clamping towards preserving history.
  - Reduce α near clamping to soften changes.
  - Track spatial contrast changes to detect flickering.

### 5.4. Undersampling Artifacts
- **Cause:** Low sample count in newly visible or rapidly changing regions (e.g., after camera cuts).
- **Mitigations:**
  - Increase spatial reuse (e.g., low-pass filter on new samples).
  - Use screen-space AA (e.g., FXAA, SMAA) on raw samples.
  - Adaptive supersampling in shaders for critical regions.

---

## 6. Performance

- **TAA is efficient:** Typically a single compute or pixel shader pass, with cost scaling with output resolution.
- **Stable cost:** Scene-independent, unlike MSAA which depends on geometry and shading complexity.
- **Temporal upsampling:** May require some post-processing passes to run at upsampled resolution, affecting performance.

---

## 7. Related Techniques

- **Variable Rate Shading (VRS):** Coarse pixel shading combined with TAA to reconstruct fine details.
- **Temporal Denoising:** TAA-like techniques denoise stochastic effects (ray tracing, SSAO, SSR) by reusing temporal data.
- **Machine Learning:** Deep learning methods are being explored to improve history rectification and denoising, surpassing hand-engineered heuristics.

---

## 8. Conclusion and Future Directions

- **TAA is highly effective and widely adopted, but not a solved problem.**
- **Main challenges:** Robustness of history rectification, handling of dynamic scenes, and balancing sharpness with stability.
- **Future work:** Advances in both analytical and machine learning-based solutions are expected to further improve TAA quality and robustness.

---

## 9. References

The article provides an extensive bibliography covering foundational and recent work in TAA, temporal upsampling, denoising, and related real-time rendering techniques. See the original article for full details.

---

# Appendix: Key Equations and Pseudocode

- **Sample Accumulation:**
  - `f_n(p) = α * s_n(p) + (1-α) * f_{n-1}(π(p))`
- **Optimal Blending:**
  - Track per-pixel sample count N, set α = 1/N for equal weighting.
- **Neighborhood Clamping:**
  - For each pixel, gather 3x3 neighborhood, compute min/max (or convex hull) in color space, clamp history color to this range.
- **Temporal Upsampling:**
  - For each output pixel, compute weighted sum of input samples using a reconstruction filter, blend with history using confidence factor β(p).

---

# Practical Implementation Notes

- **Motion Vectors:** Accurate motion vectors are critical for correct reprojection and disocclusion detection.
- **Depth/Normal/Object ID Buffers:** Used for robust history validation.
- **Color Space:** YCoCg or similar decorrelated spaces can improve clamping quality.
- **Adaptive Parameters:** α, neighborhood size, and clamping thresholds may be tuned based on motion, scene content, or user preference.

---

**This summary is intended as a technical reference for graphics programmers and researchers implementing or studying TAA and related techniques.** 