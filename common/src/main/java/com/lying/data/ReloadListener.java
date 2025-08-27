package com.lying.data;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;

public interface ReloadListener<T> extends ResourceReloader
{
	public CompletableFuture<T> load(ResourceManager manager);
	
	public CompletableFuture<Void> apply(T data, ResourceManager manager, Executor executor);
	
	public default CompletableFuture<Void> reload(ResourceReloader.Synchronizer synchronizer, ResourceManager manager, Executor prepareExecute, Executor applyExecute)
	{
		CompletableFuture<T> load = load(manager);
		return load.thenCompose(synchronizer::whenPrepared).thenCompose(t -> apply(t, manager, applyExecute));
	}
	
	public default String getName() { return getId().toString(); }
	
	public Identifier getId();
}
