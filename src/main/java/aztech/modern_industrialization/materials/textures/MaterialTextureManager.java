/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package aztech.modern_industrialization.materials.textures;

import aztech.modern_industrialization.MIRuntimeResourcePack;
import aztech.modern_industrialization.mixin.ResourceImplAccessor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import aztech.modern_industrialization.util.ResourceUtil;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

public class MaterialTextureManager {
    private final ResourceManager rm;
    private final MIRuntimeResourcePack texturePack;
    private final List<Runnable> endRunnables = new ArrayList<>();

    MaterialTextureManager(ResourceManager rm, MIRuntimeResourcePack texturePack) {
        this.rm = rm;
        this.texturePack = texturePack;
    }

    public boolean hasAsset(String asset) {
        return rm.containsResource(new Identifier(asset)) || texturePack.contains(ResourceType.CLIENT_RESOURCES, new Identifier(asset));
    }

    public NativeImage getAssetAsTexture(String textureId) throws IOException {
        if (rm.containsResource(new Identifier(textureId))) {
            Resource texture = rm.getResource(new Identifier(textureId));
            return NativeImage.read(new ByteArrayInputStream(ResourceUtil.getBytes(texture)));
        } else if (texturePack.contains(ResourceType.CLIENT_RESOURCES, new Identifier(textureId))) {
            return NativeImage.read(texturePack.open(ResourceType.CLIENT_RESOURCES, new Identifier(textureId)));
        } else {
            throw new IOException("Couldn't find texture " + textureId);
        }
    }

    /**
     * Add texture if it's not already loaded, but doesn't close the image.
     */
    public void addTexture(String textureId, NativeImage image) throws IOException {
        Identifier id = new Identifier(textureId);
        if (!rm.containsResource(id)) {
            texturePack.addAsset(textureId.replace(':', '/'), image.getBytes());
        }
    }

    public void runAtEnd(Runnable runnable) {
        endRunnables.add(runnable);
    }

    void onEnd() {
        for (Runnable runnable : endRunnables) {
            runnable.run();
        }
        endRunnables.clear();
    }
}
