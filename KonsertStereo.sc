
///////////Ljudkälla////////////////////
//aBus -- gator main
//bBus -- gator slave
//outBus -- no gator
///////////RoutingSynth////////////////////
//panBusOut -- skickar ljud till stereo genom \stereoOut
//panBusOutAz -- skickar ljud till \panAzStereo
//panBusOutPdef -- skickar ljud till \PdefOut som spelas av en Pbindef(\panning)

KonsertStereo {
	var server;
	var synth, device, <synthGroup, <fxGroup, <panGroup, <voiceBuf, <aBus, <revBusVox, <bBus, <outBus, <revBus, <panBusOut, <midiBus, delBus, gator_s, out_s, korg_s, moog_s, mic_s, klank_s, reverb_s, delay_s, panner_s;
	var fxAssignments;

	*new { //klassmetod
		^super.new.initKonsertStereo;
	}

	initKonsertStereo { //instansmetod
		server = Server.default;
		MIDIClient.init;
		server.options.device = "UltraLite-mk4";
		try {
			midiBus = MIDIOut.new(1);
			CmdPeriod.add({(0..127).do{arg n; midiBus.noteOff(0,n)}});
		} {
			server.options.device = "Built-in"
		};

		server.options.sampleRate = 48000;

		server.waitForBoot{
			KonsertStereo.sendSynthDefs(server.options.device);
			server.sync;
			~bluntNoiseBuf = Buffer.readChannel(server,"/Users/adele21/Music/Music/BluntNoise.wav", channels:[0]);
			~lostBitsDrumsBuf = Buffer.readChannel(server,"/Users/adele21/Music/Music/lostbits-drums-mono.wav", channels:[0]);
			~toPiecesChoirBuf = Buffer.readChannel(server, "/Users/adele21/Music/Music/topieces_choir.wav", channels:[0,1]);
			~toPiecesChoirBufRev = Buffer.readChannel(server, "/Users/adele21/Music/Music/topieces_choir.wav", channels:[0]);
			voiceBuf = Buffer.alloc(server, server.sampleRate * 90, 1);
			aBus = Bus.audio(server, 1); // master1
			bBus = Bus.audio(server, 1); // slave
			revBus = Bus.audio(server, 1); //reverb
			revBusVox = Bus.audio(server, 1); //reverb
			panBusOut = Bus.audio(server, 1);
			outBus = Bus.audio(server, 1); //routing passed gator through
			delBus = Bus.audio(server, 1); //delay
			synthGroup = Group.new(server);
			fxGroup = Group.after(synthGroup);
			panGroup = Group.after(fxGroup);
			server.options.numOutputBusChannels = 10;
			server.options.numInputBusChannels = 4;
			server.recChannels = 8;
		};
	}

	gator {|parValEvent|
		fork{
			if(gator_s.isNil, {
				gator_s = Synth(\gator, nil, fxGroup, 'addToHead');
				server.sync;
				parValEvent.keysValuesDo{|par, val|
					gator_s.set(par, val);
				};
			}, {
				parValEvent.keysValuesDo{|par, val|
					gator_s.set(par, val);
				};
			});
		};
	}

	*sendSynthDefs {|device|
		(
			if(device == "UltraLite-mk4", {
				SynthDef.new(\mic,{
					arg inBus, outBus, outBusRev, amp=1;
					var input;
					input = SoundIn.ar(inBus) * amp;
					Out.ar(outBus, input);
					Out.ar(outBusRev, input);
				}).add;

				SynthDef.new(\micStereo,{
					arg inBus, outBus, amp=1, revAmp=0.5, outBusRev, highPass=400;
					var input, sig, rmod;
					input = SoundIn.ar(inBus) * amp;
					sig = HPF.ar(input, highPass);
					Out.ar(outBus, sig);
					Out.ar(outBusRev, sig * revAmp);
				}).add;

				SynthDef.new(\moog, {
					arg inBus, outBus, amp=1;
					var input, filter, filter0;
					input = SoundIn.ar(inBus) * amp;
					filter = BBandStop.ar(input, 400, 2);
					filter0 = LPF.ar(filter, 1000);
					Out.ar(outBus, filter0);
				}).add;

				SynthDef.new(\D1200,{
					arg inBus, outBus, amp=1;
					var input;
					input = SoundIn.ar(inBus) * amp;
					Out.ar(outBus, input);
				}).add;
			});

			SynthDef(\playBackBuf, {
				arg outBus=0, buf, done=2, loop=0, amp=1, rate=1;
				var buffer;
				buffer = PlayBuf.ar(2, buf, BufRateScale.kr(buf) * rate, loop:loop, doneAction:done) * amp;
				Out.ar(outBus, buffer);
			}).add;

			SynthDef(\playBackBufMono, {
				arg outBus=0, buf, done=2, loop=1, amp=1, rate=1;
				var buffer;
				buffer = PlayBuf.ar(1, buf, BufRateScale.kr(buf) * rate, loop:loop, doneAction:done) * amp;
				Out.ar(outBus, buffer);
			}).add;

			SynthDef(\klank, {
				arg ut=0, i_freq=200, atk=0.1, rel=0.6, amp1=0.5, val=466.16;
				var klank, n, harm, amp, ring, env;
				env = EnvGen.ar(Env.perc(atk, rel), doneAction:2);
				harm = \harm.ir(Array.series(4, 1, 1));
				amp = \amp.ir(Array.fill(4, 0.05));
				ring = \ring.ir(Array.fill(4, 1));
				klank = Klank.ar(`[harm, amp, ring], {SinOsc.ar(val)*0.03}.dup, i_freq) * env * amp1;
				Out.ar(ut, klank);
			}).add;

			SynthDef(\gator,  {
				arg inBusA, inBusB, gate=1, lag=6, clampTime=0.01, relaxTime=0.1,
				thresh=0.5, slopeBelow=3, slopeAbove=1, outBus=0, delOut, revAmp=0.5, spread=1, lfrate=0.05;
				var control, input, snd, sndV, env, outSig;
				env = EnvGen.kr(Env.asr(), gate, doneAction: 2);
				control = In.ar(inBusA, 1);
				input = In.ar(inBusB, 1);
				snd = Compander.ar(input, control, thresh, slopeBelow, slopeAbove, clampTime.lag(lag), relaxTime.lag(lag));
				outSig = Splay.ar([control + snd], spread, center:LFTri.kr(lfrate));
				Out.ar(outBus, outSig);
			}).add;

			SynthDef(\reverb, {
				arg inBus, outBus, level=0.1, gate=1;
				var input, env, rev;
				env = EnvGen.ar(Env.asr(), gate, doneAction: 2);
				input = In.ar(inBus, 1) * env * level.lag(10);
				input = Pan2.ar(input);
				rev = NHHall.ar(input, 5, 0.5, 200, 0.5, 4000, 0.1, 0.5, 0.5, 0.2, 0.3);
				Out.ar(outBus, rev);
			}).add;

			SynthDef(\reverbVox, {
				arg inBusVox, outBusVox, level=0.1, gate=1;
				var inputVox, env, revVox;
				env = EnvGen.ar(Env.asr(), gate, doneAction: 2);
				inputVox = In.ar(inBusVox, 1) * env * level.lag(10);
				inputVox = Pan2.ar(inputVox);
				revVox = NHHall.ar(inputVox, 5, 0.5, 200, 0.5, 4000, 0.1, 0.5, 0.5, 0.2, 0.3);
				Out.ar(outBusVox, revVox);
			}).add;

			SynthDef(\panAzStereo, {
				arg inBus, outBus, speed=0.01, level=1, dir=1, width=2;
				var pan, input;
				input = In.ar(inBus) * level;
				pan = PanAz.ar(2, input, LFSaw.kr(speed) * dir, width: width);
				Out.ar(outBus, pan);
			}).add;

			SynthDef(\PdefOut, {
				arg inBus, freq, gate=1;
				var input, env;
				env = EnvGen.kr(Env.asr(), gate, doneAction: 2);
				input = In.ar(inBus) * env;
				Out.ar(freq, input);
			}).add;

			SynthDef(\stereoOut, {
				arg inBus, outBus, gate=1, center=0.0;
				var input, env, signal;
				env = EnvGen.kr(Env.asr(), gate, doneAction: 2);
				input = In.ar(inBus, 1) * env;
				signal = Splay.ar(input, center: center);
				Out.ar(outBus, signal);
			}).add;

			SynthDef(\noGator, {
				arg inBus, outBus, revOut, revInpAmp=0.5;
				var input;
				input = In.ar(inBus);
				Out.ar(outBus, input);
				Out.ar(revOut, input*revInpAmp);
			}).add;

		)
	}
}
