package org.tmarchuk.wurmunlimited.client.rendertweaks;

/**
 * Created by Tyson Marchuk on 2016-03-29.
 */

// From Wurm Unlimited Client
import com.wurmonline.client.console.WurmConsole;
import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.terrain.weather.WeatherControls;

// From Ago's modloader
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

// Base Java
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientRenderTweaksMod implements WurmMod, Initable
{
    private static final Logger logger = Logger.getLogger(ClientRenderTweaksMod.class.getName());
    private boolean renderGrass = true;
    private boolean renderTrees = true;

    public static void logException(String msg, Throwable e)
    {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }
    @Override
    public void init() {
        try
        {

            HookManager.getInstance().registerHook(
                    "com.wurmonline.client.console.WurmConsole", "handleInput2", "(Ljava/lang/String;Z)V",
                    new InvocationHandlerFactory()
                    {
                        @Override
                        public InvocationHandler createInvocationHandler()
                        {
                            return new InvocationHandler()
                            {
                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                                {
                                    synchronized (proxy)
                                    {
                                        String string = String.valueOf(args[0]);
                                        if (string.startsWith("weather"))
                                        {
                                            WurmConsole console = (WurmConsole) proxy;
                                            World world = ReflectionUtil.getPrivateField(console, ReflectionUtil.getField(console.getClass(), "world"));
                                            world.getWeather().setIgnoreWeather(true);
                                            (new WeatherControls(world)).start();

                                            return true;
                                        }
                                        else if(string.startsWith("toggleGrass"))
                                        {
                                            renderGrass = !renderGrass;
                                            return true;
                                        }
                                        else if(string.startsWith("toggleTrees"))
                                        {
                                            renderTrees = !renderTrees;
                                            return true;
                                        }

                                        return method.invoke(proxy, args);
                                    }
                                }
                            };
                        }
                    });

            // Toggle drawing of grass.
            // com.wurmonline.client.renderer.terrain.decorator.DecorationRenderer.renderGrass(Frustum frustum, Lightmap lightmap)
            ClassPool classPool = HookManager.getInstance().getClassPool();
            String descriptor;
            descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
                    classPool.get("com.wurmonline.client.renderer.Frustum"),
                    classPool.get("com.wurmonline.client.renderer.terrain.Lightmap")
            });

            HookManager.getInstance().registerHook("com.wurmonline.client.renderer.terrain.decorator.DecorationRenderer", "renderGrass", descriptor,
                    new InvocationHandlerFactory()
                    {
                        @Override
                        public InvocationHandler createInvocationHandler()
                        {
                            return new InvocationHandler()
                            {
                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                                {
                                    if (renderGrass)
                                    {
                                        // Do the normal thing if the user hasn't disabled grass.
                                        return method.invoke(proxy, args);
                                    }
                                    else
                                    {
                                        // Do nothing.
                                        return null;
                                    }
                                }
                            };
                        }
                    });
            // END - com.wurmonline.client.renderer.terrain.decorator.DecorationRenderer.renderGrass(Frustum frustum, Lightmap lightmap)

            // Toggle rendering of trees.
            // com.wurmonline.client.renderer.WorldRender.renderCells(Frustum frustum, boolean renderTrees, boolean renderObjects)
            descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
                    classPool.get("com.wurmonline.client.renderer.Frustum"),
                    CtClass.booleanType, CtClass.booleanType
            });

            HookManager.getInstance().registerHook("com.wurmonline.client.renderer.WorldRender", "renderCells", descriptor,
                    new InvocationHandlerFactory()
                    {
                        @Override
                        public InvocationHandler createInvocationHandler()
                        {
                            return new InvocationHandler()
                            {
                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                                {
                                    if (renderTrees)
                                    {
                                        // Do the normal thing if the user hasn't disabled trees.
                                        return method.invoke(proxy, args);
                                    }
                                    else
                                    {
                                        // Build a new set of arguments with trees set to false.
                                        Object[] newArgs = {args[0], false, args[2]};
                                        return method.invoke(proxy, newArgs);
                                    }
                                }
                            };
                        }
                    });
            // END - com.wurmonline.client.renderer.WorldRender.renderCells(Frustum frustum, boolean renderTrees, boolean renderObjects)

        }
        catch (NotFoundException e)
        {
            logException("Failed to create hooks for " + ClientRenderTweaksMod.class.getName(), e);
            throw new HookException(e);
        }
    }
}
