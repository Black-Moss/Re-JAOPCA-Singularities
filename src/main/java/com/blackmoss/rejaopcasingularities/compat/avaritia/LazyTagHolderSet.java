package com.blackmoss.rejaopcasingularities.compat.avaritia;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.mojang.datafixers.util.Either;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

public class LazyTagHolderSet implements HolderSet<Item> {
	private final TagKey<Item> tag;

	public LazyTagHolderSet(TagKey<Item> tag) {
		this.tag = tag;
	}

	public TagKey<Item> getTag() {
		return tag;
	}

	private Iterable<Holder<Item>> items() {
		return BuiltInRegistries.ITEM.getTagOrEmpty(tag);
	}

	@Override
	public @NonNull Iterator<Holder<Item>> iterator() {
		return items().iterator();
	}

	@Override
	public @NonNull Stream<Holder<Item>> stream() {
		return StreamSupport.stream(items().spliterator(), false);
	}

	@Override
	public int size() {
		int size = 0;
		for(Holder<Item> ignored : items()) {
			++size;
		}
		return size;
	}

	@Override
	public boolean isBound() {
		return BuiltInRegistries.ITEM.get(tag).map(Named::isBound).orElse(false);
	}

	@Override
	public @NonNull Either<TagKey<Item>, List<Holder<Item>>> unwrap() {
		return Either.left(tag);
	}

	@Override
	public @NonNull Optional<Holder<Item>> getRandomElement(@NonNull RandomSource random) {
		return Util.getRandomSafe(stream().toList(), random);
	}

	@Override
	public @NonNull Holder<Item> get(int index) {
		int i = 0;
		for(Holder<Item> holder : items()) {
			if(i++ == index) {
				return holder;
			}
		}
		throw new IndexOutOfBoundsException("Index "+index+" out of bounds for tag "+tag);
	}

	@Override
	public boolean contains(Holder<Item> value) {
		return value.is(tag);
	}

	@Override
	public boolean canSerializeIn(@NonNull HolderOwner<Item> owner) {
		return true;
	}

	@Override
	public @NonNull Optional<TagKey<Item>> unwrapKey() {
		return Optional.of(tag);
	}

	@Override
	public boolean isImmediatelyResolvable() {
		return false;
	}

	@Override
	public String toString() {
		return "LazyTagHolderSet["+tag+"]";
	}
}
