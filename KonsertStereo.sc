
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
	var synth, device, <synthGroup, <fxGroup, <panGroup, <voiceBuf, <aBus, <outBus1, <outBus2, <bBus, <sawBus, <outBus, <revBus, <panBusOutSaw, <panBusOut, <panBusOutAz, <panBusOutD, <midiBus, delBus, masterBus, masterSynth, gator_s, out_s, korg_s, moog_s, mic_s, klank_s, reverb_s, delay_s, panner_s;
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
			voiceBuf = Buffer.alloc(server, server.sampleRate * 90, 1);
			aBus = Bus.audio(server, 1); // master1
			outBus1 = Bus.audio(server, 1); // master1 out
			outBus2 = Bus.audio(server, 1); // master2 out
			bBus = Bus.audio(server, 1); // slave
			sawBus = Bus.audio(server, 1); // saw to reverb
			revBus = Bus.audio(server, 1); //reverb
			panBusOutSaw = Bus.audio(server, 1); //panning for saw
			panBusOut = Bus.audio(server, 1); //panning to all channels
			panBusOutAz = Bus.audio(server, 1); //panning circle
			panBusOutD = Bus.audio(server, 1); //panning discrete
			outBus = Bus.audio(server, 1); //routing passed gator through reverb
			delBus = Bus.audio(server, 1); //delay
			masterBus  = Bus.audio(server, 4);
			synthGroup = Group.new(server);
			fxGroup = Group.after(synthGroup);
			panGroup = Group.after(fxGroup);
			masterSynth = Synth.after(panGroup, \master, [\inBus, 0]);
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
					arg inBus, outBus, amp=1, freq=293.67, modMix=0;
					var input, sig, rmod;
					input = SoundIn.ar(inBus) * amp;
					rmod = input * SinOsc.ar(freq);
					sig = input + (rmod * modMix);
					sig = HPF.ar(sig, 400);
					Out.ar(outBus, sig);
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
				arg outBus=0, bufNum, start=0, done=2, freq=800, loop=1, pos=(-1), centerFreq=2000, bw=1, amp=1;
				var buffer, outSig;
				buffer = PlayBuf.ar(1, bufNum, BufRateScale.kr(bufNum), 1, start, loop, doneAction:done) * amp;
				buffer = BBandPass.ar(buffer, centerFreq, bw);
				Out.ar(outBus, buffer);
			}).add;

			SynthDef(\klank, {
				arg outBus, i_freq=200, atk=0.1, rel=0.6, amp1=0.5, val=466.16;
				var klank, n, harm, amp, ring, env;
				env = EnvGen.ar(Env.perc(atk, rel), doneAction:2);
				harm = \harm.ir(Array.series(4, 1, 1));
				amp = \amp.ir(Array.fill(4, 0.05));
				ring = \ring.ir(Array.fill(4, 1));
				klank = Klank.ar(`[harm, amp, ring], {SinOsc.ar(val)*0.03}.dup, i_freq) * env * amp1;
				Out.ar(outBus, klank);
			}).add;

			SynthDef(\saw, {
				arg gate=1, amp=0.01, freq=440, atk=0.1, sus=1, rel=0.5, freqF=800, outBus;
				var env, sig, filter;
				env = EnvGen.kr(Env.asr(atk, sus, rel), gate, doneAction:2);
				sig = Saw.ar(freq) * amp;
				filter = LPF.ar(sig, freqF);
				sig = filter * env;
				Out.ar(outBus, sig);
			}).add;

			SynthDef(\gator,  {
				arg inBusA, inBusB, gate=1, lag=6, clampTime=0.01, relaxTime=0.1, thresh=0.5,
				slopeBelow=3, slopeAbove=1, outBus, revOut, delOut, revAmp=0.5;
				var control, input, snd, env, outSig;
				env = EnvGen.kr(Env.asr(), gate, doneAction: 2);
				control = In.ar(inBusA, 1);
				input = In.ar(inBusB, 1);
				snd = Compander.ar(input, control, thresh, slopeBelow, slopeAbove, clampTime.lag(lag), relaxTime.lag(lag));
				outSig = (control + snd) * env;
				Out.ar(outBus, outSig);
				Out.ar(revOut, outSig*revAmp);
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

			SynthDef(\delay, {
				arg inBus, freq, atk=0.1, rel=0.5, maxdel=1.0, deltime=0.3, decay=0.5, level=0.5, gate=1;
				var delay, input, env, klick;
				env = EnvGen.kr(Env.asr(atk, 1, rel), gate, doneAction:2);
				input = In.ar(inBus) * level * env;
				delay = CombL.ar(input, maxdel, deltime, decay);
				Out.ar(freq, delay);
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
				var input, signal, env;
				env = EnvGen.kr(Env.asr(), gate, doneAction: 2);
				input = In.ar(inBus, 1) * env;
				signal = input;
				signal = Splay.ar(signal, center: center);
				Out.ar(outBus, signal);
			}).add;

			SynthDef(\noGator, {
				arg inBus, outBus, revOut, revInpAmp=0.5;
				var input;
				input = In.ar(inBus);
				Out.ar(outBus, input);
				Out.ar(revOut, input*revInpAmp);
			}).add;


			SynthDef(\master, {
				arg inBus, level=0.8;
				var sig;
				sig = In.ar(inBus, 8);
				sig = Limiter.ar(sig, level);
				Out.ar(0, sig);
			}).add;
		)
	}
}
